"""Etapas 1, 3 e 4 do saneamento (secao 9.1): identificar layout, mapear colunas, validar tipos/obrigatorios/tamanho."""
import pandas as pd

COLUNAS = ["gs", "cf", "cs", "desc", "um"]
LAYOUT_ESPERADO = {            # palavra-chave que deve aparecer no cabecalho de cada coluna
    "gs": "grupo", "cf": "fornecedor", "cs": "servi", "desc": "descri", "um": "unidade",
}
GS_VALIDOS = ["Eletromecânica", "Facilities", "Civil"]
LIMITE_SAP = 40


def identificar_layout(xlsx) -> dict:
    """Confere abas e cabecalhos. Levanta ValueError se o arquivo nao tem o layout esperado."""
    abas = pd.read_excel(xlsx, sheet_name=None, header=None, nrows=2)
    nomes = list(abas)
    if len(nomes) < 2:
        raise ValueError(f"Layout inesperado: esperadas >=2 abas (base e unidades), encontradas {nomes}")
    cab = [str(x) for x in abas[nomes[0]].iloc[0].tolist()]
    if len(cab) != 5:
        raise ValueError(f"Layout inesperado: aba base com {len(cab)} colunas (esperado 5): {cab}")
    faltando = [k for k, kw in LAYOUT_ESPERADO.items() if kw not in cab[COLUNAS.index(k)].lower()]
    if faltando:
        raise ValueError(f"Cabecalhos nao conferem para {faltando}: {cab}")
    return {"abas": nomes, "abaBase": nomes[0], "abaUnidades": nomes[1],
            "abaMaoDeObra": nomes[2] if len(nomes) > 2 else None, "cabecalhosBase": cab,
            "mapeamento": dict(zip(COLUNAS, cab))}


def validar_entrada(base: pd.DataFrame, siglas_um: set):
    """Devolve (base_valida, rejeitadas, relatorio). Erros bloqueiam a LINHA; avisos so sao registrados."""
    b = base.copy()
    erros = pd.Series([""] * len(b), index=b.index)
    def marca(mask, msg):
        nonlocal erros
        erros = erros.where(~mask, erros + msg + "; ")
    marca(b[["gs", "cf", "cs", "desc", "um"]].isna().any(axis=1), "campo obrigatorio vazio")
    marca(~b["gs"].isin(GS_VALIDOS), "grupo de servico desconhecido")
    for c in ("cf", "cs"):
        num = pd.to_numeric(b[c], errors="coerce")
        marca(num.isna() | (num != num.round()) | (num <= 0), f"{c} nao e inteiro positivo")
    marca(b["desc"].astype(str).str.strip().eq("") | b["desc"].astype(str).str.lower().eq("nan"), "descricao vazia")
    marca(~b["um"].isin(siglas_um), "unidade de medida fora da tabela")
    dup = b.duplicated(["gs", "cf", "cs", "um"], keep="first")
    marca(dup, "chave (gs,cf,cs,um) duplicada")
    ruim = erros != ""
    avisos = {
        "descricoes_acima_de_40": int((b["desc"].astype(str).str.strip().str.len() > LIMITE_SAP).sum()),
        "descricoes_com_40_exatos": int((b["desc"].astype(str).str.strip().str.len() == LIMITE_SAP).sum()),
        "espacos_nbsp": int(b["desc"].astype(str).str.contains("\xa0").sum()),
    }
    # descricao > 40 e erro de dado (limite do SAP)
    acima = b["desc"].astype(str).str.strip().str.len() > LIMITE_SAP
    erros = erros.where(~acima, erros + "descricao acima de 40 caracteres; ")
    ruim = erros != ""
    rej = b[ruim].copy(); rej["motivoRejeicao"] = erros[ruim].str.strip()
    ok = b[~ruim].reset_index(drop=True)
    rel = {"linhasEntrada": int(len(b)), "linhasValidas": int(len(ok)), "linhasRejeitadas": int(len(rej)),
           "avisos": avisos, "status": "OK" if len(rej) == 0 else "COM_REJEICOES"}
    return ok, rej, rel
