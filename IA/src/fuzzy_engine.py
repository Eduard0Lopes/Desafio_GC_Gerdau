"""Motor de similaridade (espelhado em service/similaridade/FuzzyEngine.java).

score = 0.4 * Jaccard(tokens) + 0.6 * SequenceMatcher.ratio()
As entradas devem estar normalizadas com ``normalizacao.normalizar_texto``.
"""
from collections import defaultdict
from difflib import SequenceMatcher

PESO_TOKENS = 0.4
PESO_SEQUENCIA = 0.6


def jaccard(a: str, b: str) -> float:
    tokens_a, tokens_b = set(a.split()), set(b.split())
    if not tokens_a or not tokens_b:
        return 0.0
    return len(tokens_a & tokens_b) / len(tokens_a | tokens_b)


def similaridade(a: str, b: str) -> float:
    a, b = str(a), str(b)
    j = jaccard(a, b)
    if not a.split() or not b.split():
        return 0.0
    return j * PESO_TOKENS + SequenceMatcher(None, a, b).ratio() * PESO_SEQUENCIA


class IndiceSimilaridade:
    """Índice invertido por token para evitar a comparação de todos contra todos.

    Só compara descrições que compartilham ao menos um token não muito frequente.
    Tokens presentes em mais de ``max_frequencia`` descrições (ex.: "sv") não geram candidatos.
    """

    def __init__(self, itens: dict[str, str], max_frequencia: int = 400):
        self.itens = itens
        self._indice: dict[str, list[str]] = defaultdict(list)
        for chave, texto in itens.items():
            for token in set(texto.split()):
                self._indice[token].append(chave)
        self._max_frequencia = max_frequencia

    def candidatos(self, texto: str) -> set[str]:
        encontrados: set[str] = set()
        for token in set(texto.split()):
            chaves = self._indice.get(token, ())
            if len(chaves) <= self._max_frequencia:
                encontrados.update(chaves)
        return encontrados

    def mais_similar(self, texto: str, ignorar: str | None = None) -> tuple[str | None, float]:
        melhor_chave, melhor_score = None, 0.0
        tokens = set(texto.split())
        for chave in sorted(self.candidatos(texto)):
            if chave == ignorar:
                continue
            outro = self.itens[chave]
            outros_tokens = set(outro.split())
            j = len(tokens & outros_tokens) / len(tokens | outros_tokens)
            matcher = SequenceMatcher(None, texto, outro)
            if j * PESO_TOKENS + matcher.real_quick_ratio() * PESO_SEQUENCIA <= melhor_score:
                continue
            score = j * PESO_TOKENS + matcher.ratio() * PESO_SEQUENCIA
            if score > melhor_score:
                melhor_chave, melhor_score = chave, score
        return melhor_chave, melhor_score
