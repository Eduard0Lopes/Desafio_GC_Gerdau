from difflib import SequenceMatcher

class FuzzyEngine:
    @staticmethod
    def calcular_similaridade_tokens(a, b):
        """
        6.3 & 6.4: Similaridade baseada em tokens (palavras) e fuzzy search.
        Garante que inversões de palavras (ex: 'mecanico manutencao') tenham boa correspondência.
        """
        tokens_a = set(str(a).split())
        tokens_b = set(str(b).split())
        
        if not tokens_a or not tokens_b:
            return 0.0
        
        intersecao = tokens_a.intersection(tokens_b)
        uniao = tokens_a.union(tokens_b)
        
        # Índice Jaccard para os tokens
        jaccard = len(intersecao) / len(uniao) if uniao else 0.0
        # SequenceMatcher para aproximação de caracteres (Fuzzy Search)
        sequence_ratio = SequenceMatcher(None, str(a), str(b)).ratio()
        
        # Combinação ponderada (40% tokens, 60% fuzzy)
        return (jaccard * 0.4) + (sequence_ratio * 0.6)