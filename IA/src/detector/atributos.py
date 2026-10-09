import re


def extrair_numeros(texto: str) -> list[str]:
    """
    Extrai números presentes em uma descrição.

    Aceita números inteiros e decimais utilizando
    ponto ou vírgula como separador decimal.
    """

    if not texto:
        return []

    padrao = r"\d+(?:[.,]\d+)?"

    return re.findall(padrao, str(texto))


def normalizar_numero(numero: str) -> str:
    """
    Padroniza a representação de um número.

    Exemplos:
        '16,0' -> '16'
        '16.0' -> '16'
        '050'  -> '50'
    """

    numero = numero.replace(",", ".")

    try:
        valor = float(numero)

        if valor.is_integer():
            return str(int(valor))

        return str(valor)

    except ValueError:
        return numero


def extrair_atributos_numericos(texto: str) -> list[dict]:
    """
    Extrai atributos formados por número + unidade.

    Exemplos:
        'CABO 16,0 MM²' -> [{'valor': '16', 'unidade': 'mm'}]
        'TUBO 150 MM'   -> [{'valor': '150', 'unidade': 'mm'}]
        'CONTAINER 50L' -> [{'valor': '50', 'unidade': 'l'}]
    """

    if not texto:
        return []

    texto = str(texto).lower()

    # Aceita unidades comuns encontradas em descrições técnicas.
    unidades = r"""
        mm²|mm2|mm|cm|km|m|kg|g|mg|l|ml|
        h|hs|hora|horas|min|minuto|minutos|
        un|und|unidade|pct|%
    """

    padrao = rf"(\d+(?:[.,]\d+)?)\s*({unidades})\b"

    encontrados = re.findall(
        padrao,
        texto,
        flags=re.IGNORECASE | re.VERBOSE
    )

    atributos = []

    for valor, unidade in encontrados:
        atributos.append(
            {
                "valor": normalizar_numero(valor),
                "unidade": unidade.lower()
            }
        )

    return atributos


def comparar_atributos(texto1: str, texto2: str) -> dict:
    """
    Compara os atributos numéricos de duas descrições.

    Retorna os atributos encontrados em cada descrição
    e indica se existem diferenças.
    """

    atributos1 = extrair_atributos_numericos(texto1)
    atributos2 = extrair_atributos_numericos(texto2)

    return {
        "atributos1": atributos1,
        "atributos2": atributos2,
        "atributos_diferentes": atributos1 != atributos2
    }


def comparar_numeros(texto1: str, texto2: str) -> dict:
    """
    Mantém a comparação simples de números para compatibilidade
    com os testes anteriores.
    """

    numeros1 = extrair_numeros(texto1)
    numeros2 = extrair_numeros(texto2)

    numeros1_normalizados = [
        normalizar_numero(numero)
        for numero in numeros1
    ]

    numeros2_normalizados = [
        normalizar_numero(numero)
        for numero in numeros2
    ]

    return {
        "numeros1": numeros1_normalizados,
        "numeros2": numeros2_normalizados,
        "numeros_diferentes": (
            numeros1_normalizados != numeros2_normalizados
        )
    }