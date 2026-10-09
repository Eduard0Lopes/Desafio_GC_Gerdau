
from normalizacao import normalizar_texto
from fuzzy import (
    calcular_similaridade,
    classificar_similaridade,
)
from atributos import comparar_atributos


def analisar_descricoes(
    descricao1: str,
    descricao2: str,
    limite_similaridade: float = 70.0,
) -> dict:
    texto1 = normalizar_texto(descricao1)
    texto2 = normalizar_texto(descricao2)

    similaridade = calcular_similaridade(texto1, texto2)
    classificacao_textual = classificar_similaridade(similaridade)

    resultado_atributos = comparar_atributos(
        descricao1,
        descricao2,
    )

    tipo_diferenca = resultado_atributos["tipo_diferenca"]
    atributos_diferentes = resultado_atributos["atributos_diferentes"]
    candidato = similaridade >= limite_similaridade

    if not candidato:
        classificacao_prioridade = "Baixa"
        recomendacao = "Fora do limite inicial de busca"

    elif tipo_diferenca == "Valores diferentes para a mesma unidade":
        classificacao_prioridade = "Alta"
        recomendacao = "Revisar valores diferentes para a mesma unidade"

    elif tipo_diferenca == "Atributos ou unidades diferentes":
        classificacao_prioridade = "Alta"
        recomendacao = "Revisar atributos ou unidades diferentes"

    elif tipo_diferenca == "Atributo ausente em uma descrição":
        classificacao_prioridade = "Média"
        recomendacao = "Verificar se falta um atributo na descrição"

    elif texto1 == texto2:
        classificacao_prioridade = "Média"
        recomendacao = (
            "Verificar possível duplicidade e comparar os dados cadastrais"
        )

    else:
        classificacao_prioridade = (
            "Média" if similaridade >= 85 else "Baixa"
        )
        recomendacao = "Comparar outros dados antes de concluir"

    return {
        "descricao_original_1": descricao1,
        "descricao_original_2": descricao2,
        "descricao_normalizada_1": texto1,
        "descricao_normalizada_2": texto2,
        "similaridade": round(similaridade, 2),
        "classificacao_textual": classificacao_textual,
        "candidato": candidato,
        "atributos_1": resultado_atributos["atributos1"],
        "atributos_2": resultado_atributos["atributos2"],
        "atributos_diferentes": atributos_diferentes,
        "tipo_diferenca_atributos": tipo_diferenca,
        "classificacao_prioridade": classificacao_prioridade,
        "recomendacao": recomendacao,
    }