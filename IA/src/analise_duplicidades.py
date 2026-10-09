
import pandas as pd

from rapidfuzz import fuzz, process

from .detector.normalizacao import normalizar_descricao, extrair_numeros


COLUNAS_RELATORIO = [
    "Descrição do Item",
    "Grupo de Serviço",
    "Código do Fornecedor",
    "Unidade medida",
    "Quantidade de códigos",
    "Códigos de serviço",
    "Quantidade de descrições originais",
    "Classificação inicial",
]

COLUNAS_PARES_SEMELHANTES = [
    "Similaridade (%)",
    "Descrição 1",
    "Códigos de serviço 1",
    "Descrição 2",
    "Códigos de serviço 2",
    "Grupo de Serviço",
    "Código do Fornecedor",
    "Unidade medida",
    "Comparação numérica",
    "Classificação inicial",
]

COLUNAS_OBRIGATORIAS = [
    "Descrição do Item",
    "Grupo de Serviço",
    "Código do Fornecedor",
    "Unidade medida",
    "Código do Serviço",
]


def _preparar_base_duplicidades(df):
    """Valida e prepara os dados para as análises de duplicidade."""

    colunas_ausentes = [
        coluna
        for coluna in COLUNAS_OBRIGATORIAS
        if coluna not in df.columns
    ]

    if colunas_ausentes:
        raise ValueError(
            "Colunas obrigatórias ausentes: "
            + ", ".join(colunas_ausentes)
        )

    base = df[COLUNAS_OBRIGATORIAS].copy()

    base["Descricao_Normalizada"] = (
        base["Descrição do Item"]
        .fillna("")
        .apply(normalizar_descricao)
    )

    colunas_contexto = [
        "Grupo de Serviço",
        "Código do Fornecedor",
        "Unidade medida",
    ]

    for coluna in colunas_contexto:
        base[coluna] = (
            base[coluna]
            .fillna("")
            .astype(str)
            .str.strip()
        )

    base["_codigo"] = (
        base["Código do Serviço"]
        .fillna("")
        .astype(str)
        .str.strip()
    )

    base = base[
        (base["Descricao_Normalizada"] != "")
        & (base["_codigo"] != "")
    ].copy()

    return base


def gerar_relatorio_duplicidades(df):
    """
    Identifica descrições normalizadas iguais no mesmo contexto,
    associadas a mais de um código de serviço.

    Os resultados são candidatos para revisão humana,
    não duplicidades confirmadas.
    """

    base = _preparar_base_duplicidades(df)

    colunas_agrupamento = [
        "Descricao_Normalizada",
        "Grupo de Serviço",
        "Código do Fornecedor",
        "Unidade medida",
    ]

    relatorio = (
        base.groupby(
            colunas_agrupamento,
            dropna=False,
        )
        .agg(
            Quantidade_de_codigos=("_codigo", "nunique"),
            Codigos=(
                "_codigo",
                lambda valores: sorted(valores.unique().tolist()),
            ),
            Descricoes_originais=("Descrição do Item", "nunique"),
            Descricao_original=("Descrição do Item", "first"),
        )
        .reset_index()
    )

    relatorio = relatorio[
        relatorio["Quantidade_de_codigos"] > 1
    ].copy()

    if relatorio.empty:
        return pd.DataFrame(columns=COLUNAS_RELATORIO)

    relatorio["Classificacao_inicial"] = (
        "Possível duplicidade — revisar"
    )

    relatorio = relatorio.rename(
        columns={
            "Descricao_original": "Descrição do Item",
            "Quantidade_de_codigos": "Quantidade de códigos",
            "Codigos": "Códigos de serviço",
            "Descricoes_originais": (
                "Quantidade de descrições originais"
            ),
            "Classificacao_inicial": "Classificação inicial",
        }
    )

    relatorio = relatorio[COLUNAS_RELATORIO]

    relatorio = (
        relatorio.sort_values(
            by="Quantidade de códigos",
            ascending=False,
        )
        .reset_index(drop=True)
    )

    return relatorio


def buscar_pares_descricoes_semelhantes(
    df,
    similaridade_minima=75,
    limite_por_descricao=20,
):
    """
    Busca descrições diferentes, mas textualmente semelhantes,
    dentro do mesmo grupo, fornecedor e unidade de medida.

    Os resultados são candidatos para revisão humana.

    Parâmetros:
        df: DataFrame original dos serviços.
        similaridade_minima: pontuação mínima, entre 0 e 100.
        limite_por_descricao: máximo de correspondências
            retornadas para cada descrição pesquisada.

    Retorna:
        DataFrame com pares candidatos, códigos associados,
        similaridade textual e comparação numérica.
    """

    if not 0 <= similaridade_minima <= 100:
        raise ValueError(
            "similaridade_minima deve estar entre 0 e 100."
        )

    if limite_por_descricao < 1:
        raise ValueError(
            "limite_por_descricao deve ser pelo menos 1."
        )

    base = _preparar_base_duplicidades(df)

    colunas_contexto = [
        "Grupo de Serviço",
        "Código do Fornecedor",
        "Unidade medida",
    ]

    pares_encontrados = []

    # Guarda os pares já processados em um conjunto,
    # evitando buscas repetidas em uma lista crescente.
    pares_processados = set()

    agrupamentos = base.groupby(
        colunas_contexto,
        dropna=False,
        sort=False,
    )

    for contexto, grupo_contexto in agrupamentos:

        # Uma descrição normalizada aparece uma única vez
        # na lista de comparação de cada contexto.
        descricoes = sorted(
            grupo_contexto["Descricao_Normalizada"]
            .unique()
            .tolist()
        )

        if len(descricoes) < 2:
            continue

        # Prepara as informações de cada descrição uma única vez.
        informacoes = {}

        for descricao_normalizada, registros in (
            grupo_contexto.groupby(
                "Descricao_Normalizada",
                sort=False,
            )
        ):
            descricoes_originais = (
                registros["Descrição do Item"]
                .dropna()
                .astype(str)
                .drop_duplicates()
                .tolist()
            )

            codigos = sorted(
                registros.loc[
                    registros["_codigo"] != "",
                    "_codigo",
                ].unique().tolist()
            )

            descricao_original = (
                descricoes_originais[0]
                if descricoes_originais
                else descricao_normalizada
            )

            informacoes[descricao_normalizada] = {
                "descricao_original": descricao_original,
                "codigos": codigos,
                "numeros": extrair_numeros(descricao_original),
            }

        # Compara cada descrição somente com as demais
        # do mesmo grupo, fornecedor e unidade de medida.
        for indice_base, descricao_base in enumerate(descricoes):

            correspondencias = process.extract(
                descricao_base,
                descricoes[indice_base + 1:],
                scorer=fuzz.ratio,
                limit=limite_por_descricao,
                score_cutoff=similaridade_minima,
            )

            for descricao_encontrada, pontuacao, _ in correspondencias:

                descricao_1 = descricao_base
                descricao_2 = descricao_encontrada

                chave_par = (
                    tuple(contexto),
                    descricao_1,
                    descricao_2,
                )

                if chave_par in pares_processados:
                    continue

                pares_processados.add(chave_par)

                info_1 = informacoes[descricao_1]
                info_2 = informacoes[descricao_2]

                if info_1["numeros"] == info_2["numeros"]:
                    comparacao_numerica = "Números correspondentes"
                    classificacao = "Candidato para revisão"
                else:
                    comparacao_numerica = (
                        "Revisar diferenças numéricas"
                    )
                    classificacao = (
                        "Candidato para revisão — "
                        "diferenças numéricas"
                    )

                pares_encontrados.append(
                    {
                        "Similaridade (%)": round(
                            float(pontuacao),
                            1,
                        ),
                        "Descrição 1": info_1["descricao_original"],
                        "Códigos de serviço 1": info_1["codigos"],
                        "Descrição 2": info_2["descricao_original"],
                        "Códigos de serviço 2": info_2["codigos"],
                        "Grupo de Serviço": contexto[0],
                        "Código do Fornecedor": contexto[1],
                        "Unidade medida": contexto[2],
                        "Comparação numérica": comparacao_numerica,
                        "Classificação inicial": classificacao,
                    }
                )

    if not pares_encontrados:
        return pd.DataFrame(
            columns=COLUNAS_PARES_SEMELHANTES
        )

    relatorio = pd.DataFrame(pares_encontrados)

    relatorio = (
        relatorio.sort_values(
            by="Similaridade (%)",
            ascending=False,
        )
        .reset_index(drop=True)
    )

    return relatorio[COLUNAS_PARES_SEMELHANTES]