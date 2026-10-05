
import re
import unicodedata


def normalizar_descricao(texto):
    """
    Padroniza uma descrição para facilitar comparações textuais.

    Converte para minúsculas, remove acentos e espaços excedentes.
    Preserva pontuação, sinais e números.
    """
    texto = str(texto).strip().lower()

    texto = unicodedata.normalize("NFKD", texto)
    texto = "".join(
        caractere
        for caractere in texto
        if not unicodedata.combining(caractere)
    )

    texto = re.sub(r"\s+", " ", texto).strip()

    return texto


def extrair_numeros(texto):
    """
    Extrai números inteiros, decimais e frações de uma descrição.

    Exemplos: 2, 0.5, 1/2 e 3/4.
    """
    texto = str(texto).lower().replace(",", ".")
    texto = re.sub(r"\bpolegadas?\b", " pol", texto)
    texto = texto.replace('"', " pol")

    padrao = r"\d+(?:\.\d+)?(?:/\d+(?:\.\d+)?)?"

    return re.findall(padrao, texto)