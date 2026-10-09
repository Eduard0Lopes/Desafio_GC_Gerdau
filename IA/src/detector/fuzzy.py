from rapidfuzz import fuzz


def calcular_similaridade(texto1: str, texto2: str) -> float:
    """
    Calcula a similaridade entre dois textos utilizando
    comparação fuzzy baseada nos termos presentes.

    Retorna um valor entre 0 e 100.
    """

    if not texto1 or not texto2:
        return 0.0

    return fuzz.token_set_ratio(texto1, texto2)


def classificar_similaridade(similaridade: float) -> str:
    """
    Classifica o nível de similaridade entre duas descrições.

    Faixas:
        90 a 100 -> Muito semelhante
        80 a 89  -> Semelhante
        70 a 79  -> Possível semelhança
        abaixo de 70 -> Baixa semelhança
    """

    if similaridade >= 90:
        return "Muito semelhante"

    if similaridade >= 80:
        return "Semelhante"

    if similaridade >= 70:
        return "Possível semelhança"

    return "Baixa semelhança"