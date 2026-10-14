"""Etapa 2: gera as coleções NoSQL (data/processed/nosql/*.json) consumidas pela API Java.

Os documentos seguem exatamente os modelos em com.gerdau.desafio.model e são importados
pelo DadosIaSeeder (app.seed.enabled=true).
"""
import json

import pandas as pd

import config
from dados import carregar_base_saneada, gerar_id, ids_servico, mapear_por_ordem
from normalizacao import limpar_espacos, normalizar_texto


def carregar_unidades() -> pd.DataFrame:
    if config.BASE_ORIGINAL.exists():
        return pd.read_excel(config.BASE_ORIGINAL, sheet_name=config.ABA_UNIDADES)
    return pd.read_csv(config.UNIDADES_REFERENCIA)


def unicos_em_ordem(valores) -> list[str]:
    return list(dict.fromkeys(str(v) for v in valores))


def montar_snapshot(grupo: str, codigo_sap: int, descricao: str, sigla: str) -> str:
    """Mesmo formato de CodigoCompletoService.montarSnapshot no backend."""
    return f"{grupo} | Fornecedor {codigo_sap} | {descricao} | {sigla}"


def gerar_documentos(df: pd.DataFrame, df_um: pd.DataFrame) -> dict[str, list[dict]]:
    map_gs = mapear_por_ordem(df["Grupo de Serviço"], "gs")
    map_cf = mapear_por_ordem(df["Código do Fornecedor"], "cf")
    map_cs = ids_servico(df)

    grupos = [
        {"_id": gs_id, "nome": nome, "nomeNormalizado": normalizar_texto(nome), "origem": "MIGRACAO"}
        for nome, gs_id in map_gs.items()
    ]
    fornecedores = [
        {
            "_id": cf_id,
            "codigoFornecedorSAP": int(codigo),
            "nome": f"Fornecedor ID {int(codigo)}",
            "nomeNormalizado": f"fornecedor id {int(codigo)}",
            "origem": "MIGRACAO",
        }
        for codigo, cf_id in map_cf.items()
    ]

    unidades, map_um = [], {}
    for i, row in enumerate(df_um.itertuples(index=False), start=1):
        sigla, nome = str(row[0]).strip().upper(), limpar_espacos(row[1])
        um_id = gerar_id(i, "um")
        map_um[sigla] = um_id
        unidades.append({"_id": um_id, "sigla": sigla, "nome": nome,
                         "nomeNormalizado": normalizar_texto(nome), "origem": "MIGRACAO"})

    df = df.assign(
        gsId=df["Grupo de Serviço"].map(map_gs),
        cfId=df["Código do Fornecedor"].map(map_cf),
        csId=df["DescricaoNormalizada"].map(map_cs),
        sigla=df["Unidade medida"].astype(str).str.strip().str.upper(),
    )
    desconhecidas = sorted(set(df["sigla"]) - set(map_um))
    if desconhecidas:
        raise ValueError(f"Unidades de medida sem cadastro: {desconhecidas}")
    df["umId"] = df["sigla"].map(map_um)

    servicos, descricao_por_cs = [], {}
    for cs_id, grupo in df.groupby("csId", sort=True):
        primeira = grupo.iloc[0]
        descricao = limpar_espacos(primeira["DescricaoOriginal"])
        descricao_por_cs[cs_id] = descricao
        servicos.append({
            "_id": cs_id,
            "descricao": descricao,
            "descricaoNormalizada": primeira["DescricaoNormalizada"],
            "quantidadeCaracteres": len(descricao),
            "origem": "MIGRACAO",
            "legacyIds": unicos_em_ordem(grupo["Código do Serviço"]),
        })

    completos = []
    df["idCompleto"] = df["gsId"] + df["cfId"] + df["csId"] + df["umId"]
    nome_gs = {v: k for k, v in map_gs.items()}
    for id_completo, grupo in df.groupby("idCompleto", sort=True):
        r = grupo.iloc[0]
        completos.append({
            "_id": id_completo,
            "idCompleto": id_completo,
            "gsId": r["gsId"],
            "cfId": r["cfId"],
            "csId": r["csId"],
            "umId": r["umId"],
            "descricaoSnapshot": montar_snapshot(nome_gs[r["gsId"]], int(r["Código do Fornecedor"]),
                                                 descricao_por_cs[r["csId"]], r["sigla"]),
            "usuarioCriacaoId": None,
            "createdAt": None,
            "origem": "MIGRACAO",
            "legacyIds": unicos_em_ordem(grupo["Código do Serviço"]),
        })

    return {
        "grupos_servico": grupos,
        "unidades_medida": unidades,
        "codigos_fornecedor": fornecedores,
        "codigos_servico": servicos,
        "codigos_completos": completos,
    }


def salvar(colecoes: dict[str, list[dict]]):
    config.NOSQL.mkdir(parents=True, exist_ok=True)
    for nome, docs in colecoes.items():
        with open(config.NOSQL / f"{nome}.json", "w", encoding="utf-8") as f:
            json.dump(docs, f, ensure_ascii=False, indent=2)
        print(f"     {nome}: {len(docs)} documentos")


def main():
    colecoes = gerar_documentos(carregar_base_saneada(), carregar_unidades())
    print(f"[OK] Coleções NoSQL geradas em {config.NOSQL}")
    salvar(colecoes)


if __name__ == "__main__":
    main()
