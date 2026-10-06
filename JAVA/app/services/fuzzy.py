import unicodedata
import re
from typing import List, Dict, Any, Tuple

def normalizar_texto(texto: str) -> str:
    """
    Remove acentos, converte para minúsculas e remove caracteres especiais/espaços extras.
    """
    if not texto:
        return ""
    # Normalização NFD para decompor acentos
    nfkd = unicodedata.normalize('NFD', texto)
    sem_acento = "".join([c for c in nfkd if unicodedata.category(c) != 'Mn'])
    # Minúsculas e substituição de caracteres não alfanuméricos por espaço
    limpo = re.sub(r'[^a-zA-Z0-9\s]', ' ', sem_acento.lower())
    # Remover espaços duplicados
    return " ".join(limpo.split())

def calcular_levenshtein_distance(str1: str, str2: str) -> int:
    """Calcula a distância de Levenshtein entre duas strings."""
    if len(str1) < len(str2):
        return calcular_levenshtein_distance(str2, str1)

    if len(str2) == 0:
        return len(str1)

    previous_row = range(len(str2) + 1)
    for i, c1 in enumerate(str1):
        current_row = [i + 1]
        for j, c2 in enumerate(str2):
            insertions = previous_row[j + 1] + 1
            deletions = current_row[j] + 1
            substitutions = previous_row[j] + (c1 != c2)
            current_row.append(min(insertions, deletions, substitutions))
        previous_row = current_row

    return previous_row[-1]

def calcular_similaridade(str1: str, str2: str) -> float:
    """
    Retorna percentual de similaridade de 0.0 a 1.0 (0 a 100%).
    Combina distância de Levenshtein com verificação de substrings/tokens.
    """
    norm1 = normalizar_texto(str1)
    norm2 = normalizar_texto(str2)

    if not norm1 or not norm2:
        return 0.0

    if norm1 == norm2:
        return 1.0

    max_len = max(len(norm1), len(norm2))
    dist = calcular_levenshtein_distance(norm1, norm2)
    base_score = 1.0 - (dist / max_len)

    # Recompensa para coincidência de tokens
    tokens1 = set(norm1.split())
    tokens2 = set(norm2.split())
    if tokens1 and tokens2:
        intersection = tokens1.intersection(tokens2)
        jaccard = len(intersection) / len(tokens1.union(tokens2))
        final_score = (base_score * 0.6) + (jaccard * 0.4)
    else:
        final_score = base_score

    return round(final_score, 4)

def validar_limite_sap(texto: str, limite_max: int = 40) -> Tuple[bool, int]:
    """
    Valida a regra rígida de até 40 caracteres do SAP.
    """
    tamanho = len(texto) if texto else 0
    valido = tamanho <= limite_max
    return valido, tamanho
