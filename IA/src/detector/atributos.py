
import re
from normalizacao import normalizar_texto

def extrair_numeros(texto: str) -> list[str]:
    if not texto:
        return []
    padrao = r"\d+(?:[.,]\d+)?"
    return re.findall(padrao, str(texto))


def normalizar_numero(numero: str) -> str:
    numero = numero.replace(",", ".")
    try:
        valor = float(numero)
        if valor.is_integer():
            return str(int(valor))
        return str(valor)
    except ValueError:
        return numero


def normalizar_unidade(unidade: str) -> str:
    unidade = unidade.lower().strip()

    equivalencias = {
        "hs": "h",
        "hora": "h",
        "horas": "h",
        "minuto": "min",
        "minutos": "min",
        "mm2": "mm²",
        "und": "un",
        "unidade": "un",
    }

    return equivalencias.get(unidade, unidade)


def extrair_atributos_numericos(texto: str) -> list[dict]:
    if not texto:
        return []

    texto = str(texto).lower()

    unidades = (
        r"mm²|mm2|mm|cm|km|kg|mg|ml|"
        r"minutos|minuto|min|horas|hora|hs|h|"
        r"unidade|und|un|pct|%|"
        r"m|g|l"
    )

    padrao = rf"(\d+(?:[.,]\d+)?)\s*({unidades})\b"

    encontrados = re.findall(
        padrao,
        texto,
        flags=re.IGNORECASE,
    )

    atributos = []

    for valor, unidade in encontrados:
        valor_normalizado = float(
            normalizar_numero(valor)
        )
        unidade_normalizada = normalizar_unidade(unidade)

        # Converte litros para mililitros para permitir
        # a comparação entre unidades equivalentes.
        if unidade_normalizada == "l":
            valor_normalizado *= 1000
            unidade_normalizada = "ml"

        if valor_normalizado.is_integer():
            valor_final = str(int(valor_normalizado))
        else:
            valor_final = str(valor_normalizado)

        atributos.append({
            "valor": valor_final,
            "unidade": unidade_normalizada,
        })

    return atributos


def comparar_atributos(texto1: str, texto2: str) -> dict:
    atributos1 = extrair_atributos_numericos(texto1)
    atributos2 = extrair_atributos_numericos(texto2)

    if atributos1 == atributos2:
        tipo_diferenca = "Nenhuma"

    elif not atributos1 or not atributos2:
        tipo_diferenca = "Atributo ausente em uma descrição"

    else:
        unidades1 = [
            atributo["unidade"] for atributo in atributos1
        ]
        unidades2 = [
            atributo["unidade"] for atributo in atributos2
        ]

        if unidades1 == unidades2:
            tipo_diferenca = (
                "Valores diferentes para a mesma unidade"
            )
        else:
            tipo_diferenca = (
                "Atributos ou unidades diferentes"
            )

    return {
        "atributos1": atributos1,
        "atributos2": atributos2,
        "atributos_diferentes": atributos1 != atributos2,
        "tipo_diferenca": tipo_diferenca,
    }


def comparar_numeros(texto1: str, texto2: str) -> dict:
    numeros1 = [
        normalizar_numero(numero)
        for numero in extrair_numeros(texto1)
    ]
    numeros2 = [
        normalizar_numero(numero)
        for numero in extrair_numeros(texto2)
    ]

    return {
        "numeros1": numeros1,
        "numeros2": numeros2,
        "numeros_diferentes": numeros1 != numeros2,
    }



def comparar_caracteristicas_textuais(
    texto1: str,
    texto2: str,
) -> list[str]:
    texto1 = normalizar_texto(texto1)
    texto2 = normalizar_texto(texto2)

    caracteristicas_opostas = [
        ("com gas", "sem gas", "presença de gás"),
        ("diurno", "noturno", "período do serviço"),
        ("armado", "desarmado", "condição de armamento"),
    ]

    diferencas = []

    for termo1, termo2, descricao in caracteristicas_opostas:
        presente1 = termo1 in texto1
        presente2 = termo2 in texto1
        presente3 = termo1 in texto2
        presente4 = termo2 in texto2

        if (presente1 and presente4) or (presente2 and presente3):
            diferencas.append(descricao)

    return diferencas