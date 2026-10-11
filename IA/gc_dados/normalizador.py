"""Normalizacao e decomposicao de descricoes de servico.

Cada descricao vira: funcao + especificidade (taxonomia da aba 'Mao de Obra')
+ modificadores + numeros. Dois itens so sao comparaveis quando especificidade,
modificadores e numeros coincidem (ver similaridade.py).
"""
import json
import re
import unicodedata
from difflib import SequenceMatcher
from pathlib import Path

DIC_PATH = Path(__file__).resolve().parent.parent / "dicionario.json"
LIMITE_SAP = 40
VERSAO_ALGORITMO = "1.1.0"   # incrementar ao mudar regras de normalizacao (registrado em cada lote, secao 9.2)

# token ja expandido -> flag semantica
_FLAGS = {
    "extra": "HE", "noturno": "NOT", "domingo": "DOMFER", "feriado": "DOMFER",
    "segunda": "SEGSAB", "sabado": "SEGSAB", "sexta": "SEGSAB",
    "periculosidade": "PERIC", "insalubridade": "INSAL", "mensal": "MENSAL",
    "mobilizacao": "MOB", "desmobilizacao": "DESMOB", "retroativo": "RETRO",
}
_RUIDO = {"adicional"}  # consumido sem gerar flag
_RE_NUM = re.compile(r"^\d+(d\d+)?%?$")
_HORARIAS = {"HE", "NOT", "PERIC", "INSAL", "HN"}


def carregar_dicionario(path=DIC_PATH):
    """Le o dicionario e aplica sobrescritas por variavel de ambiente (secao 17: thresholds fora do codigo)."""
    import os
    dic = json.loads(Path(path).read_text(encoding="utf-8"))
    for chave, env in (("provavel", "GC_LIMIAR_PROVAVEL"), ("alerta", "GC_LIMIAR_ALERTA"),
                       ("coesao_minima_cluster", "GC_COESAO_MINIMA")):
        if os.environ.get(env):
            dic["limiares"][chave] = float(os.environ[env])
    return dic


def limpar(texto) -> str:
    """minusculas, sem acentos, sem pontuacao, espacos unicos; preserva numeros e %."""
    s = str(texto).replace("\xa0", " ")
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    for op, nome in (("<=", " menorigual "), (">=", " maiorigual "), ("<", " menor "), (">", " maior ")):
        s = s.replace(op, nome)                        # operadores mudam o sentido (D > 1/2 x D <= 1/2)
    s = re.sub(r"(\d)\s*x\s*(\d)", r"\1 x \2", s)      # 12x36
    s = re.sub(r"(\d)(?=[a-z])", r"\1 ", s)            # 50mm -> 50 mm
    s = re.sub(r"(?<=\d)[.,](?=\d)", "d", s)           # 0,65 -> 0d65
    s = re.sub(r"(\d)\s+%", r"\1%", s)
    s = re.sub(r"[^a-z0-9% ]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def _corrigir_token(t: str, dic):
    """prefixos conhecidos (pericul..) e correcao de digitacao das palavras-chave de especificidade."""
    for pre, alvo in dic.get("prefixos", {}).items():
        if t.startswith(pre): return alvo
    if len(t) >= 7 and t not in dic.get("vocab_fuzzy", ()):
        melhor = max(dic.get("vocab_fuzzy", ()), key=lambda v: SequenceMatcher(None, t, v).ratio(), default=None)
        if melhor and SequenceMatcher(None, t, melhor).ratio() >= 0.88:
            return melhor
    return t


def expandir(limpo: str, dic) -> list:
    """aplica regras de frase e expande abreviacoes; devolve lista de tokens."""
    s = f" {limpo} "
    for padrao, subst in dic["frases"]:
        s = re.sub(padrao, subst, s)
    toks = []
    for t in s.split():
        if t not in dic["tokens"] and not t.isdigit():
            t = _corrigir_token(t, dic)
        toks.extend(dic["tokens"].get(t, t).split())
    return toks


def analisar(desc, dic) -> dict:
    texto = str(desc)
    limpo = limpar(texto)
    toks = expandir(limpo, dic)
    stop = set(dic["stopwords"])
    papeis = set(dic["papeis"])
    mods_map = dic["modificadores"]

    flags, mods, nums, resto = set(), set(), [], []
    i = 0
    while i < len(toks):
        t = toks[i]
        nxt = toks[i + 1] if i + 1 < len(toks) else None
        if t == "sem" and nxt in ("insalubridade", "periculosidade"):
            flags.add("SEM_" + nxt[:5].upper()); i += 2; continue
        if t == "hora" and nxt == "normal":
            flags.add("HN"); i += 2; continue
        if t in _FLAGS:
            flags.add(_FLAGS[t]); i += 1; continue
        if t in _RUIDO:
            i += 1; continue
        if t in mods_map:
            mods.add(mods_map[t]); i += 1; continue
        if _RE_NUM.match(t):
            nums.append(t.rstrip("%").replace("d", ".")); i += 1; continue
        if t in stop:
            i += 1; continue
        resto.append(t); i += 1
    if flags & _HORARIAS:
        resto = [t for t in resto if t != "hora"]

    eh_papel = any(t in papeis for t in toks)
    espec, conf = classificar(flags, eh_papel, mods)
    unicos = list(dict.fromkeys(resto))          # sem repeticao (ex.: 'Adm' + sufixo '-ADM')
    chave = "|".join([
        "+".join(sorted(flags)), "+".join(sorted(mods)),
        " ".join(sorted(nums)), " ".join(sorted(unicos)),
    ])
    return {
        "descricao": texto,
        "descricaoNormalizada": limpo,
        "descricaoExpandida": " ".join(toks),
        "funcao": unicos, "flags": sorted(flags), "modificadores": sorted(mods),
        "numeros": sorted(nums), "chave": chave,
        "especificidade": espec, "confiancaEspecificidade": conf,
        "ehMaoDeObra": espec != "Nao e mao de obra",
        "quantidadeCaracteres": len(texto.strip()),
        "possivelTruncamento": len(texto.strip()) >= LIMITE_SAP,
        "tokensAbreviadosUsados": sum(1 for t in limpo.split() if t in dic["tokens"]),
    }


def classificar(f: set, tem_papel: bool, mods=frozenset()):
    """mapeia o conjunto de flags para uma das 14 categorias da aba 'Mao de Obra'."""
    if "RETRO" in f: return "Retroativo", "ALTA"
    if f & {"MOB", "DESMOB"}: return "Mobilizacao / Desmobilizacao", "ALTA"
    if "PERIC" in f:
        if "MENSAL" in f: return "Adicional periculosidade (mensal)", "ALTA"
        if "HE" in f: return "Adicional periculosidade s/ hora extra", "ALTA"
        return "Adicional periculosidade (hora)", "ALTA"
    if "INSAL" in f: return "Adicional insalubridade (hora)", "ALTA"
    if "HE" in f:
        dia = "DOMFER" in f
        explicito = dia or "SEGSAB" in f
        if "NOT" in f:
            nome = "Hora extra noturna Dom/Feriado" if dia else "Hora extra noturna Seg a Sab"
        else:
            nome = "Hora extra Dom/Feriado" if dia else "Hora extra Seg a Sab"
        return nome, ("ALTA" if explicito else "MEDIA")
    if "NOT" in f and ("DOMFER" in f or "SEGSAB" in f):   # 'C/ Ad Not (Dom e Fer)' sem 'HE': assumido HE noturna
        return ("Hora extra noturna Dom/Feriado" if "DOMFER" in f else "Hora extra noturna Seg a Sab"), "MEDIA"
    if "NOT" in f: return "Adicional noturno (hora)", "ALTA"
    if "HN" in f: return "Hora normal", "ALTA"
    if "MENSAL" in f and tem_papel and "DEMANDA" not in mods: return "Posto mensal (equipe fixa)", "MEDIA"
    if tem_papel: return "Mao de obra - especificidade nao informada", "BAIXA"
    return "Nao e mao de obra", "MEDIA"


def descricao_sugerida(a: dict, dic):
    """Proposta 'FUNCAO - ESPECIFICIDADE [pct] [modificadores]' (<=40). None se nao couber."""
    rot = dic["rotulos_curtos"].get(a["especificidade"])
    if not rot or not (set(a["funcao"]) & set(dic["papeis"])):
        return None
    if a["especificidade"].startswith("Hora extra") and a["numeros"]:
        rot += f" {a['numeros'][0]}%"
    s = f"{' '.join(a['funcao']).upper()} - {rot}"
    if a["modificadores"]:
        s += " " + " ".join(a["modificadores"])
    return s if len(s) <= LIMITE_SAP else None
