"""Auditoria dos artefatos gerados: volume, integridade referencial e contrato com a API Java.

Encerra com código 1 se alguma verificação falhar.
"""
import json
import re
import sys

import pandas as pd

import config
from dados import carregar_base_saneada
from normalizacao import normalizar_texto

FORMATO_ID = {
    "grupos_servico": r"\d{3}",
    "unidades_medida": r"\d{2}",
    "codigos_fornecedor": r"\d{3}",
    "codigos_servico": r"\d{6}",
    "codigos_completos": r"\d{14}",
}


def carregar_colecoes() -> dict[str, list[dict]]:
    colecoes = {}
    for nome in config.COLECOES:
        with open(config.NOSQL / f"{nome}.json", encoding="utf-8") as f:
            colecoes[nome] = json.load(f)
    return colecoes


def verificar(base: pd.DataFrame, colecoes: dict[str, list[dict]]) -> list[str]:
    falhas = []

    def checar(condicao, mensagem):
        print(f"  [{'OK' if condicao else 'FALHA'}] {mensagem}")
        if not condicao:
            falhas.append(mensagem)

    if config.BASE_ORIGINAL.exists():
        original = pd.read_excel(config.BASE_ORIGINAL, sheet_name=config.ABA_BASE)
        checar(len(original) == len(base), f"Base saneada preserva os {len(original)} registros da original")

    checar((base["DescricaoNormalizada"].fillna("") == base["DescricaoOriginal"].map(normalizar_texto)).all(),
           "DescricaoNormalizada da base saneada segue normalizacao.normalizar_texto")

    for nome, docs in colecoes.items():
        ids = [d["_id"] for d in docs]
        checar(len(ids) == len(set(ids)), f"{nome}: {len(ids)} documentos com _id único")
        checar(all(re.fullmatch(FORMATO_ID[nome], i) for i in ids), f"{nome}: _id no formato {FORMATO_ID[nome]}")

    for nome, campo in [("grupos_servico", "nome"), ("unidades_medida", "nome")]:
        checar(all(d["nomeNormalizado"] == normalizar_texto(d[campo]) for d in colecoes[nome]),
               f"{nome}: nomeNormalizado consistente com a normalização")

    servicos = colecoes["codigos_servico"]
    checar(len(servicos) == base["DescricaoNormalizada"].nunique(),
           "codigos_servico: um documento por descrição normalizada única")
    checar(all(d["quantidadeCaracteres"] == len(d["descricao"]) for d in servicos),
           "codigos_servico: quantidadeCaracteres == len(descricao)")

    ids = {nome: {d["_id"] for d in docs} for nome, docs in colecoes.items()}
    completos = colecoes["codigos_completos"]
    referencias_ok = all(
        c["gsId"] in ids["grupos_servico"] and c["cfId"] in ids["codigos_fornecedor"]
        and c["csId"] in ids["codigos_servico"] and c["umId"] in ids["unidades_medida"]
        for c in completos
    )
    checar(referencias_ok, "codigos_completos: todas as referências GS/CF/CS/UM existem")
    checar(all(c["_id"] == c["gsId"] + c["cfId"] + c["csId"] + c["umId"] == c["idCompleto"] for c in completos),
           "codigos_completos: _id == idCompleto == GS+CF+CS+UM")

    checar(len({l for c in completos for l in c["legacyIds"]}) == base["Código do Serviço"].astype(str).nunique(),
           "codigos_completos: todo código legado da base está rastreável via legacyIds")
    return falhas


def main():
    print("Auditoria dos artefatos do pipeline IA/Dados")
    falhas = verificar(carregar_base_saneada(), carregar_colecoes())
    if falhas:
        print(f"\n{len(falhas)} verificação(ões) falharam.")
        sys.exit(1)
    print("\nTodas as verificações passaram.")


if __name__ == "__main__":
    main()
