
import pandas as pd
from rapidfuzz import process, fuzz

from .detector.normalizacao import normalizar_descricao, extrair_numeros


COLUNAS_RESULTADO = [
    "Similaridade (%)",
    "Grupo de Serviço",
    "Código do Fornecedor",
    "Código do Serviço",
    "Descrição do Item",
    "Unidade medida",
]

COLUNAS_ANALISE = COLUNAS_RESULTADO + [
    "Comparação textual",
    "Comparação numérica",
    "Classificação inicial",
    "Alerta de cadastro",
]


def preparar_base(df):
    """Prepara uma cópia da base com as descrições normalizadas."""
    base = df.copy()

    if "Descricao_Normalizada" not in base.columns:
        base["Descricao_Normalizada"] = (
            base["Descrição do Item"]
            .fillna("")
            .apply(normalizar_descricao)
        )

    return base


def buscar_servicos(
    df,
    descricao,
    limite=10,
    similaridade_minima=60,
):
    """Busca serviços por similaridade textual."""
    return buscar_servicos_contextual(
        df=df,
        descricao=descricao,
        limite=limite,
        similaridade_minima=similaridade_minima,
    )


def buscar_servicos_contextual(
    df,
    descricao,
    grupo=None,
    fornecedor=None,
    unidade=None,
    limite=10,
    similaridade_minima=60,
):
    """Busca serviços semelhantes com filtros contextuais."""
    base = preparar_base(df)
    descricao_normalizada = normalizar_descricao(descricao)

    if not descricao_normalizada:
        return pd.DataFrame(columns=COLUNAS_RESULTADO)

    candidatos = base.copy()

    if grupo is not None:
        candidatos = candidatos[
            candidatos["Grupo de Serviço"]
            .fillna("")
            .astype(str)
            .str.strip()
            .str.casefold()
            == str(grupo).strip().casefold()
        ]

    if fornecedor is not None:
        candidatos = candidatos[
            candidatos["Código do Fornecedor"]
            .fillna("")
            .astype(str)
            .str.strip()
            == str(fornecedor).strip()
        ]

    if unidade is not None:
        candidatos = candidatos[
            candidatos["Unidade medida"]
            .fillna("")
            .astype(str)
            .str.strip()
            .str.casefold()
            == str(unidade).strip().casefold()
        ]

    if candidatos.empty:
        return pd.DataFrame(columns=COLUNAS_RESULTADO)

    descricoes = (
        candidatos["Descricao_Normalizada"]
        .drop_duplicates()
        .tolist()
    )

    correspondencias = process.extract(
        descricao_normalizada,
        descricoes,
        scorer=fuzz.ratio,
        limit=limite,
        score_cutoff=similaridade_minima,
    )

    resultados = []

    for descricao_encontrada, pontuacao, _ in correspondencias:
        registros = candidatos[
            candidatos["Descricao_Normalizada"]
            == descricao_encontrada
        ]

        for _, registro in registros.iterrows():
            resultados.append({
                "Similaridade (%)": round(pontuacao, 1),
                "Grupo de Serviço": registro["Grupo de Serviço"],
                "Código do Fornecedor": registro[
                    "Código do Fornecedor"
                ],
                "Código do Serviço": registro["Código do Serviço"],
                "Descrição do Item": registro["Descrição do Item"],
                "Unidade medida": registro["Unidade medida"],
            })

    if not resultados:
        return pd.DataFrame(columns=COLUNAS_RESULTADO)

    return (
        pd.DataFrame(resultados)
        .drop_duplicates()
        .sort_values("Similaridade (%)", ascending=False)
        .head(50)
        .reset_index(drop=True)
    )



def verificar_alerta_cadastro(base, registro):
    """
    Identifica outros códigos associados à mesma descrição normalizada,
    priorizando ocorrências no mesmo contexto de cadastro.
    """
    descricao = normalizar_descricao(
        registro["Descrição do Item"]
    )

    mesma_descricao = base[
        base["Descricao_Normalizada"] == descricao
    ]

    if mesma_descricao.empty:
        return "Nenhum outro código identificado"

    mesma_descricao = mesma_descricao.copy()

    mesma_descricao["_codigo"] = (
        mesma_descricao["Código do Serviço"]
        .fillna("")
        .astype(str)
        .str.strip()
    )

    codigo_atual = str(
        registro["Código do Serviço"]
    ).strip()

    codigos_diferentes = mesma_descricao[
        (mesma_descricao["_codigo"] != "")
        & (mesma_descricao["_codigo"] != codigo_atual)
    ]

    if codigos_diferentes.empty:
        return "Nenhum outro código identificado"

    mesmo_grupo = (
        codigos_diferentes["Grupo de Serviço"]
        .fillna("")
        .astype(str)
        .str.strip()
        .str.casefold()
        == str(registro["Grupo de Serviço"]).strip().casefold()
    )

    mesmo_fornecedor = (
        codigos_diferentes["Código do Fornecedor"]
        .fillna("")
        .astype(str)
        .str.strip()
        == str(registro["Código do Fornecedor"]).strip()
    )

    mesma_unidade = (
        codigos_diferentes["Unidade medida"]
        .fillna("")
        .astype(str)
        .str.strip()
        .str.casefold()
        == str(registro["Unidade medida"]).strip().casefold()
    )

    mesmo_contexto = (
        mesmo_grupo
        & mesmo_fornecedor
        & mesma_unidade
    )

    if mesmo_contexto.any():
        return "Possível duplicidade no mesmo contexto"

    unidades_diferentes = (
        codigos_diferentes["Unidade medida"]
        .fillna("")
        .astype(str)
        .str.strip()
        .str.casefold()
        != str(registro["Unidade medida"]).strip().casefold()
    )

    if unidades_diferentes.any():
        return "Outros códigos em unidades diferentes"

    return "Outros códigos em contextos diferentes"

def buscar_servicos_com_analise(
    df,
    descricao,
    grupo=None,
    fornecedor=None,
    unidade=None,
    limite=10,
    similaridade_minima=60,
):
    """Busca serviços e acrescenta análises e alertas de cadastro."""
    base = preparar_base(df)

    resultados = buscar_servicos_contextual(
        df=base,
        descricao=descricao,
        grupo=grupo,
        fornecedor=fornecedor,
        unidade=unidade,
        limite=limite,
        similaridade_minima=similaridade_minima,
    )

    if resultados.empty:
        return pd.DataFrame(columns=COLUNAS_ANALISE)

    descricao_normalizada = normalizar_descricao(descricao)
    numeros_pesquisados = extrair_numeros(descricao)

    comparacoes_textuais = []
    comparacoes_numericas = []
    classificacoes = []
    alertas = []

    for _, registro in resultados.iterrows():
        descricao_encontrada = normalizar_descricao(
            registro["Descrição do Item"]
        )

        numeros_encontrados = extrair_numeros(
            registro["Descrição do Item"]
        )

        texto_exato = (
            descricao_normalizada == descricao_encontrada
        )

        numeros_iguais = (
            numeros_pesquisados == numeros_encontrados
        )

        if texto_exato:
            comparacoes_textuais.append(
                "Descrição normalizada exata"
            )
        else:
            comparacoes_textuais.append(
                "Descrição semelhante"
            )

        if numeros_iguais:
            comparacoes_numericas.append(
                "Números correspondentes"
            )
        else:
            comparacoes_numericas.append(
                "Revisar diferenças numéricas"
            )

        if texto_exato and numeros_iguais:
            classificacoes.append(
                "Correspondência textual exata"
            )
        elif not numeros_iguais:
            classificacoes.append(
                "Revisar diferenças numéricas"
            )
        else:
            classificacoes.append(
                "Candidato para revisão"
            )

        alertas.append(
            verificar_alerta_cadastro(base, registro)
        )

    resultados["Comparação textual"] = comparacoes_textuais
    resultados["Comparação numérica"] = comparacoes_numericas
    resultados["Classificação inicial"] = classificacoes
    resultados["Alerta de cadastro"] = alertas

    return resultados[COLUNAS_ANALISE]