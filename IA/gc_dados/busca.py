"""Busca textual (secoes 6.1-6.7): exata, prefixo/autocomplete, fuzzy, stemming e ranking.

Ranking (6.7), do mais forte ao mais fraco:
  1 ID exato  2 prefixo (de ID, secao 6.6)  3 texto normalizado exato  4 tokens  5 fuzzy  6 relevancia por uso/status ativo
Prefixo de TEXTO fica logo abaixo do texto exato (um texto exato nunca perde para um prefixo).
O mesmo contrato e atendido por BuscaMemoria (testes, calibracao) e BuscaAtlas (producao):
a API nao depende do mecanismo (secao 5.4 / 3 'Evolucao incremental').
Resposta enxuta (14.1): id, nome, descricao, score (+ tipoMatch para explicar o ranking).
"""
import json
import math
from collections import defaultdict
from difflib import SequenceMatcher
from pathlib import Path

from .normalizador import carregar_dicionario, expandir, limpar
from .stemmer import stem

TIPOS = {  # colecao -> (campo nome, campos de valor exato, usa legado)
    "codigos_servico": ("descricao", (), True),
    "grupos_servico": ("nome", (), False),
    "codigos_fornecedor": ("nome", ("codigoLegado",), False),
    "unidades_medida": ("nome", ("sigla",), False),
    "codigos_completos": ("descricaoSnapshot", ("idCompleto",), True),
}


class IndiceBusca:
    def __init__(self, docs, campo_nome="descricao", campos_valor=(), usa_legado=False, dic=None):
        self.dic = dic or carregar_dicionario()
        self.r = self.dic["ranking"]
        stop = set(self.dic["stopwords"])
        self.docs = docs
        self.sin = self.dic.get("sinonimos", {})          # secao 20: sinonimos (so busca; nao afeta fusao de itens)
        self.inv, self.inv_stem = defaultdict(set), defaultdict(set)
        self.por_id, self.por_legado, self.por_valor = defaultdict(set), defaultdict(set), defaultdict(set)
        self.meta = []
        for i, d in enumerate(docs):
            nome_norm = limpar(d.get(campo_nome, ""))
            toks = {self.sin.get(t, t) for t in expandir(nome_norm, self.dic) if t not in stop}
            stems = {stem(t) for t in toks}
            for t in toks: self.inv[t].add(i)
            for s in stems: self.inv_stem[s].add(i)
            did = str(d["_id"]).lower()
            self.por_id[did].add(i)
            if usa_legado:
                for l in d.get("legacyIds", []): self.por_legado[limpar(str(l)).replace(" ", "")].add(i)
            for c in campos_valor:
                if d.get(c): self.por_valor[limpar(str(d[c])).replace(" ", "")].add(i)
            self.meta.append({"id": did, "nome_norm": nome_norm, "toks": toks, "stems": stems,
                              "uso": d.get("qtdLinhasLegado", 0) or 0,
                              "ativo": d.get("status", "ATIVO") == "ATIVO"})
        self.vocab = list(self.inv)

    # ---- conjuntos de documentos por termo ----
    def _exato(self, t): return self.inv.get(t, set())
    def _stem(self, t): return self.inv_stem.get(stem(t), set())
    def _prefixo(self, t):
        out = set()
        for v in self.vocab:
            if v.startswith(t): out |= self.inv[v]
        return out
    def _substring(self, t):
        out = set()
        if len(t) >= 3:
            for v in self.vocab:
                if t in v and not v.startswith(t): out |= self.inv[v]
        return out
    def _fuzzy(self, t):
        """{doc: melhor razao} para termos do vocabulario com razao >= minima (so termos com 4+ letras)."""
        out = {}
        if len(t) < 4: return out
        for v in self.vocab:
            if abs(len(v) - len(t)) > 2: continue
            rz = SequenceMatcher(None, t, v).ratio()
            if rz >= self.r["fuzzy_razao_minima"]:
                for i in self.inv[v]: out[i] = max(out.get(i, 0), rz)
        return out

    def buscar(self, consulta: str, limite: int = 10, offset: int = 0) -> dict:
        qn = limpar(consulta)
        if not qn: return {"total": 0, "resultados": []}
        r, stop = self.r, set(self.dic["stopwords"])
        qc = qn.replace(" ", "")
        qt = [self.sin.get(t, t) for t in expandir(qn, self.dic) if t not in stop] or qn.split()
        brutos = [t for t in qn.split() if t not in stop] or qn.split()      # o que o usuario digitou (autocomplete)
        cand = {}
        def propor(i, sc, tipo):
            if i not in cand or sc > cand[i][0]: cand[i] = (sc, tipo)

        # 1) ID exato (completo, legado, valor padronizado como HR/UN/M2)
        for i in self.por_id.get(qc, ()): propor(i, r["id_exato"], "ID_EXATO")
        for i in self.por_legado.get(qc, ()): propor(i, r["id_legado_exato"], "ID_LEGADO_EXATO")
        for i in self.por_valor.get(qc, ()): propor(i, r["valor_exato"], "VALOR_EXATO")
        # 2) prefixo (do ID ou do inicio do texto)
        if len(qc) >= 2:
            if qc.isalnum() and " " not in qn:
                for i, m in enumerate(self.meta):
                    if m["id"].startswith(qc): propor(i, r["prefixo"], "PREFIXO_ID")
            for i, m in enumerate(self.meta):
                if m["nome_norm"].startswith(qn): propor(i, r["texto_exato"] - 20, "PREFIXO_TEXTO")
        # 3) texto normalizado exato (mesma sequencia, ou mesmos termos em outra ordem)
        sq = set(qt)
        for i, m in enumerate(self.meta):
            if m["nome_norm"] == qn or m["toks"] == sq: propor(i, r["texto_exato"], "TEXTO_EXATO")
        # 4) tokens: exato, stem, prefixo de palavra (autocomplete), substring
        def por_termo(bruto, fn):
            """docs que casam o termo digitado OU sua expansao (ex.: 'mec' -> 'mecanico')."""
            exp = [self.sin.get(t, t) for t in expandir(bruto, self.dic) if t not in stop]
            via_exp = set.intersection(*[fn(e) for e in exp]) if exp else set()
            return (fn(bruto) | via_exp) if len(bruto) >= 2 else via_exp
        def todos(fn):
            conjuntos = [por_termo(b, fn) for b in brutos]
            return set.intersection(*conjuntos) if conjuntos else set()
        for chave, fn, tipo in (("tokens", self._exato, "TOKENS"), ("tokens_stem", self._stem, "TOKENS_STEM"),
                                ("tokens_prefixo", self._prefixo, "TOKENS_PREFIXO"),
                                ("tokens_prefixo", self._substring, "TOKENS_SUBSTRING")):
            base = r[chave] - (20 if tipo == "TOKENS_SUBSTRING" else 0)
            for i in todos(fn):
                cobertura = min(1.0, len(qt) / max(1, len(self.meta[i]["toks"])))
                propor(i, base + 100 * cobertura * 0.4, tipo)
        # 5) fuzzy (todos os termos casam com razao >= minima)
        mapas = []
        for b in brutos:
            m = dict(self._fuzzy(b))
            for e in (self.sin.get(t, t) for t in expandir(b, self.dic)):
                for i, rz in self._fuzzy(e).items(): m[i] = max(m.get(i, 0), rz)
            mapas.append(m)
        if all(mapas):
            for i in set.intersection(*[set(m) for m in mapas]):
                media = sum(m[i] for m in mapas) / len(mapas)
                propor(i, r["fuzzy"] * media, "FUZZY")
        # 6) relevancia por uso e status ativo (desempate/bonus)
        res = []
        for i, (sc, tipo) in cand.items():
            m = self.meta[i]
            bonus = min(r["bonus_uso_max"], 10 * math.log10(1 + m["uso"])) + (r["bonus_ativo"] if m["ativo"] else 0)
            res.append((sc + bonus, tipo, i))
        res.sort(key=lambda x: (-x[0], self.meta[x[2]]["id"]))
        campo = self._campo_nome
        page = [{"id": self.docs[i]["_id"], "nome": self.docs[i].get(campo),
                 "descricao": self.docs[i].get("descricao", self.docs[i].get(campo)),
                 "score": round(sc, 2), "tipoMatch": tipo} for sc, tipo, i in res[offset: offset + limite]]
        return {"total": len(res), "resultados": page}

    _campo_nome = "descricao"


# ---------------- contrato de mecanismo de busca (troca Atlas <-> OpenSearch sem mudar a API) ----------------
class BuscaMemoria:
    """Implementacao em memoria: testes, calibracao e uso offline."""
    def __init__(self, dados: dict, dic=None):
        self.idx = {}
        for colecao, docs in dados.items():
            campo, valores, legado = TIPOS[colecao]
            ix = IndiceBusca(docs, campo, valores, legado, dic); ix._campo_nome = campo
            self.idx[colecao] = ix

    @classmethod
    def de_arquivos(cls, pasta, dic=None):
        pasta = Path(pasta); dados = {}
        for c in TIPOS:
            f = pasta / f"{c}.jsonl"
            if f.exists():
                dados[c] = [json.loads(l) for l in f.read_text(encoding="utf-8").splitlines() if l.strip()]
        return cls(dados, dic)

    def buscar(self, colecao: str, consulta: str, limite: int = 10, offset: int = 0) -> dict:
        return self.idx[colecao].buscar(consulta, limite, offset)


def pipeline_atlas(consulta: str, indice: str = "cs_busca", limite: int = 10, offset: int = 0) -> list:
    """Pipeline $search equivalente ao ranking acima (pesos = boosts). NAO testado contra um Atlas real."""
    q = consulta.strip()
    fuzzy = {"maxEdits": 1, "prefixLength": 2}
    should = [
        {"equals": {"path": "idBusca", "value": q.lower(), "score": {"boost": {"value": 30}}}},
        {"equals": {"path": "legacyIds", "value": q, "score": {"boost": {"value": 28}}}},
        {"autocomplete": {"query": q, "path": "idBusca", "score": {"boost": {"value": 20}}}},
        {"phrase": {"query": q, "path": "descricaoNormalizada", "score": {"boost": {"value": 12}}}},
        {"text": {"query": q, "path": ["descricaoNormalizada", "descricaoExpandida"], "score": {"boost": {"value": 6}}}},
        {"autocomplete": {"query": q, "path": "descricaoNormalizada", "fuzzy": fuzzy, "score": {"boost": {"value": 3}}}},
        {"text": {"query": q, "path": "descricaoNormalizada", "fuzzy": fuzzy, "score": {"boost": {"value": 1}}}},
        {"near": {"path": "qtdLinhasLegado", "origin": 100, "pivot": 50, "score": {"boost": {"value": 1}}}},   # uso
    ]
    return [
        {"$search": {"index": indice, "compound": {"should": should, "minimumShouldMatch": 1,
                                                      "filter": [{"equals": {"path": "status", "value": "ATIVO"}}]}}},
        {"$skip": offset}, {"$limit": limite},
        {"$project": {"_id": 1, "descricao": 1, "score": {"$meta": "searchScore"}}},   # so id, nome, descricao, score (14.1)
    ]


class BuscaAtlas:
    """Mesmo contrato de BuscaMemoria, sobre MongoDB Atlas Search. NAO testada neste ambiente (sem cluster)."""
    INDICES = {"codigos_servico": "cs_busca", "grupos_servico": "gs_busca", "codigos_fornecedor": "cf_busca",
               "unidades_medida": "um_busca", "codigos_completos": "cc_busca"}

    def __init__(self, db): self.db = db

    def buscar(self, colecao: str, consulta: str, limite: int = 10, offset: int = 0) -> dict:
        pipe = pipeline_atlas(consulta, self.INDICES[colecao], limite, offset)
        docs = list(self.db[colecao].aggregate(pipe))
        return {"total": len(docs), "resultados": [
            {"id": d["_id"], "nome": d.get("descricao"), "descricao": d.get("descricao"),
             "score": round(d["score"], 2), "tipoMatch": "ATLAS"} for d in docs]}


if __name__ == "__main__":
    import sys
    colecao, consulta = (sys.argv[1], " ".join(sys.argv[2:])) if sys.argv[1] in TIPOS else ("codigos_servico", " ".join(sys.argv[1:]))
    b = BuscaMemoria.de_arquivos(Path(__file__).resolve().parent.parent / "saida")
    for x in b.buscar(colecao, consulta)["resultados"]:
        print(f"{x['id']:>14} | {str(x['nome'])[:50]:50} | {x['score']:7} | {x['tipoMatch']}")
