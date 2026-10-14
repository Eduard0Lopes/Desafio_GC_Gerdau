"""Normalização textual única do projeto.

Espelhada em JAVA/backend/desafio/.../util/TextoUtil.java: qualquer mudança aqui
precisa ser replicada lá para que a busca por similaridade da API dê o mesmo resultado.
"""
import re
import unicodedata

LIMITE_SAP = 40

_NAO_ALFANUMERICO = re.compile(r"[^a-z0-9\s]")
_ESPACOS = re.compile(r"\s+")


def normalizar_texto(texto) -> str:
    """Minúsculas, sem acentos, apenas [a-z0-9] e espaços simples."""
    if texto is None or (isinstance(texto, float) and texto != texto):
        return ""
    nfd = unicodedata.normalize("NFD", str(texto))
    sem_acento = "".join(c for c in nfd if unicodedata.category(c) != "Mn")
    limpo = _NAO_ALFANUMERICO.sub(" ", sem_acento.lower())
    return _ESPACOS.sub(" ", limpo).strip()


def limpar_espacos(texto) -> str:
    return _ESPACOS.sub(" ", str(texto)).strip()
