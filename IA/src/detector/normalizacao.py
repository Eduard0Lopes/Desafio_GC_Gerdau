import re

from unidecode import unidecode


def normalizar_texto(texto: str) -> str:
    """
    Normaliza uma descrição para facilitar comparações textuais.

    Mantém o texto original intacto e produz uma versão:
    - em letras minúsculas;
    - sem acentos;
    - sem pontuação desnecessária;
    - com espaços normalizados.
    """

    if texto is None:
        return ""

    texto = str(texto).lower()

    # Remove acentos.
    texto = unidecode(texto)

    # Substitui caracteres que não sejam letras, números ou espaços.
    texto = re.sub(r"[^a-z0-9\s]", " ", texto)

    # Remove espaços duplicados.
    texto = re.sub(r"\s+", " ", texto)

    return texto.strip()