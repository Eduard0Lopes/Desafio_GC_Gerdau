def calcular_prioridade(
    similaridade: float,
    atributos_diferentes: bool
) -> float:
    """
    Calcula uma pontuação de prioridade para revisão.

    A similaridade textual representa a maior parte da pontuação.
    Quando existem diferenças nos atributos técnicos,
    o caso recebe um acréscimo de prioridade.

    Retorna um valor entre 0 e 100.
    """

    pontuacao = similaridade

    if atributos_diferentes:
        pontuacao += 10

    return min(pontuacao, 100.0)


def classificar_prioridade(pontuacao: float) -> str:
    """
    Classifica a prioridade de análise.

    Faixas:
        90 a 100 -> Alta
        80 a 89  -> Média
        abaixo de 80 -> Baixa
    """

    if pontuacao >= 90:
        return "Alta"

    if pontuacao >= 80:
        return "Média"

    return "Baixa"