"""Pontuacao de similaridade entre dois itens, com motivo explicavel.

Entrada: funcao (lista de tokens) e numeros de cada item. Os itens ja chegam
com a mesma especificidade/modificadores (bloqueio feito no pipeline).
Score = 0.4*Jaccard(tokens) + 0.6*SequenceMatcher(tokens ordenados).
Regras de seguranca: numeros diferentes limitam o score (0,60); unidade de
medida diferente desconta 0,05.
"""
from difflib import SequenceMatcher

TETO_NUMEROS_DIFERENTES = 0.60
TETO_NUMERO_AUSENTE = 0.85   # numero so em um dos itens (ex.: HE 100% x HE): alerta, nao separa
TETO_NEGACAO = 0.60          # 'sem' presente so em um dos lados (c/ x s/)
TETO_TERMO_SEM_PAR = 0.89    # termo sem correspondente (nao e erro de digitacao/truncamento)
DESCONTO_UM = 0.05
RAZAO_TYPO = 0.85


_PREFIXOS_NEGATIVOS = ("im", "in", "des", "ir", "il", "nao")


def _oposto(x, y):
    """produtiva x improdutiva: um e o outro com prefixo de negacao."""
    return any(p + x == y or p + y == x for p in _PREFIXOS_NEGATIVOS)


def _casam(x, y, trunc_a=None, trunc_b=None):
    """True se x (de A) e y (de B) sao o mesmo termo com erro de digitacao ou truncamento do SAP.
    Truncamento por prefixo so vale para o ultimo termo de uma descricao que bateu nos 40 caracteres."""
    if _oposto(x, y):
        return False
    if len(x) >= 3 and len(y) >= 3:
        if y.startswith(x): return x == trunc_a
        if x.startswith(y): return y == trunc_b
    return SequenceMatcher(None, x, y).ratio() >= RAZAO_TYPO


def alinhar(sa, sb, trunc_a=None, trunc_b=None):
    """Casa termos equivalentes (digitacao/truncamento). Devolve (sb_alinhado, pares, sem_par)."""
    so_a, so_b = sorted(sa - sb), sorted(sb - sa)
    troca, resto_b, sobra_a = {}, list(so_b), []
    for x in so_a:
        y = next((y for y in resto_b if _casam(x, y, trunc_a, trunc_b)), None)
        if y is None: sobra_a.append(x)
        else: troca[y] = x; resto_b.remove(y)
    return {troca.get(t, t) for t in sb}, troca, sobra_a + resto_b


def pontuar(fa, fb, na, nb, um_a=None, um_b=None, trunc_a=None, trunc_b=None) -> dict:
    sa, sb = set(fa), set(fb)
    sb2, troca, sem_par = alinhar(sa, sb, trunc_a, trunc_b)
    if sa == sb:
        bruto = 1.0
    elif sa == sb2:
        bruto = 0.97                     # so difere por digitacao/truncamento
    else:
        jac = len(sa & sb2) / len(sa | sb2)
        seq = SequenceMatcher(None, " ".join(sorted(sa)), " ".join(sorted(sb2))).ratio()
        bruto = 0.4 * jac + 0.6 * seq
    score = bruto
    motivos = []
    if sa == sb:
        motivos.append("mesmos termos apos normalizacao")
    else:
        comuns = sorted(sa & sb2)
        if comuns: motivos.append("em comum: " + " ".join(comuns))
        if troca: motivos.append("grafia/truncamento equivalente: " + ", ".join(f"{y}~{x}" for y, x in troca.items()))
        so_a = sorted(set(sem_par) & sa); so_b = sorted(set(sem_par) & sb)
        if so_a: motivos.append("so em A: " + " ".join(so_a))
        if so_b: motivos.append("so em B: " + " ".join(so_b))
    if ("sem" in sa) != ("sem" in sb):
        score = min(score, TETO_NEGACAO)
        motivos.append("negacao em apenas um item (com x sem)")
    elif sem_par:
        score = min(score, TETO_TERMO_SEM_PAR)
        motivos.append("termo(s) sem correspondente: " + " ".join(sorted(sem_par)))
    numeros_dif = sorted(na) != sorted(nb)
    ausente = numeros_dif and (not na or not nb)
    if ausente:
        score = min(score, TETO_NUMERO_AUSENTE)
        motivos.append(f"numero presente em apenas um item (A={list(na)} B={list(nb)})")
    elif numeros_dif:
        score = min(score, TETO_NUMEROS_DIFERENTES)
        motivos.append(f"numeros diferentes (A={list(na)} B={list(nb)})")
    um_dif = bool(um_a and um_b and um_a != um_b)
    if um_dif:
        score = max(0.0, score - DESCONTO_UM)
        motivos.append(f"unidade diferente ({um_a} x {um_b})")
    return {"score": round(score, 4), "scoreBruto": round(bruto, 4),
            "motivo": "; ".join(motivos), "numerosDiferentes": numeros_dif and not ausente, "termosSemPar": sem_par,
            "umDiferente": um_dif}


def decisao(score: float, lim) -> str:
    if score >= lim["provavel"]: return "REVISAR"
    if score >= lim["alerta"]: return "ALERTAR"
    return "PERMITIR"
