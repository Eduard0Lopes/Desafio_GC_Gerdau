"""Etapa 3: Matriz De-Para (código legado -> código de serviço canônico) com score real de similaridade.

- decision CONSOLIDADO: a descrição normalizada já existe em outro código legado e foi unificada.
- statusSimilaridade: resultado da política de ranking contra o serviço canônico mais parecido
  (descrições diferentes), usando o mesmo motor da API (/api/codigos-servico/similares).
"""
import pandas as pd

import config
from dados import carregar_base_saneada, ids_servico
from fuzzy_engine import IndiceSimilaridade
from normalizacao import limpar_espacos
from ranking_policy import classificar


def calcular_similares(descricoes_por_cs: dict[str, str]) -> dict[str, tuple[str | None, float]]:
    indice = IndiceSimilaridade(descricoes_por_cs)
    return {cs_id: indice.mais_similar(texto, ignorar=cs_id) for cs_id, texto in descricoes_por_cs.items()}


def gerar_matriz(df: pd.DataFrame) -> pd.DataFrame:
    map_cs = ids_servico(df)
    df = df.assign(newId=df["DescricaoNormalizada"].map(map_cs))
    primeiras = df.drop_duplicates("newId")
    canonica = dict(zip(primeiras["newId"], primeiras["DescricaoOriginal"].map(limpar_espacos)))
    normalizada = dict(zip(primeiras["newId"], primeiras["DescricaoNormalizada"]))
    indices_canonicos = set(primeiras.index)

    similares = calcular_similares(normalizada)

    linhas = []
    for idx, row in df.iterrows():
        cs_id = row["newId"]
        similar_id, score = similares[cs_id]
        status, acao = classificar(score)
        consolidado = idx not in indices_canonicos
        linhas.append({
            "legacyId": str(row["Código do Serviço"]),
            "legacyDescription": row["DescricaoOriginal"],
            "legacySource": config.LEGACY_SOURCE,
            "newEntityType": "CS",
            "newId": cs_id,
            "descricaoCanonica": canonica[cs_id],
            "decision": "CONSOLIDADO" if consolidado else "ORIGINAL",
            "reason": ("Duplicidade exata consolidada no código canônico" if consolidado
                       else "Registro canônico do código de serviço"),
            "similarId": similar_id or "",
            "similarDescricao": canonica.get(similar_id, ""),
            "score": round(score, 4),
            "statusSimilaridade": status,
            "acaoRecomendada": acao,
        })
    return pd.DataFrame(linhas)


def main():
    df_matriz = gerar_matriz(carregar_base_saneada())
    config.PROCESSED.mkdir(parents=True, exist_ok=True)
    df_matriz.to_excel(config.MATRIZ_XLSX, index=False)
    df_matriz.to_csv(config.MATRIZ_CSV, index=False, encoding="utf-8-sig")
    print(f"[OK] Matriz De-Para: {len(df_matriz)} linhas -> {config.MATRIZ_XLSX.name} / {config.MATRIZ_CSV.name}")
    print(df_matriz["decision"].value_counts().to_string())
    canonicos = df_matriz.drop_duplicates("newId")
    print(canonicos["statusSimilaridade"].value_counts().to_string())


if __name__ == "__main__":
    main()
