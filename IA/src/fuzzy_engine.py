from difflib import SequenceMatcher
import unicodedata

class FuzzyEngine:
    @staticmethod
    def _sem_acento_minusculo(texto):
        return "".join(
            c for c in unicodedata.normalize("NFD", str(texto).strip())
            if unicodedata.category(c) != "Mn"
        ).lower()

    @staticmethod
    def _singularizar(palavra):
        """
        Reduz a palavra ao singular provável (tolerância a plural).
        Espelha TextoUtil.singularizar do backend Java para manter a paridade
        entre os dois motores.
        """
        p = FuzzyEngine._sem_acento_minusculo(palavra)
        if len(p) < 4 or not p.isalpha():
            return p
        if p.endswith("oes") and len(p) >= 5:
            return p[:-3] + "ao"          # solucoes -> solucao
        if p.endswith("ns"):
            return p[:-2] + "m"           # bens -> bem
        if p.endswith("zes") and len(p) >= 5:
            return p[:-3] + "z"           # luzes -> luz
        if p.endswith("eis") and len(p) >= 6:
            return p[:-3] + "el"          # papeis -> papel
        if p.endswith("ais") and len(p) >= 6:
            return p[:-3] + "al"          # materiais -> material
        if p.endswith("ois") and len(p) >= 5:
            return p[:-3] + "ol"          # farois -> farol
        if p.endswith("is") and len(p) >= 5:
            return p[:-2] + "il"          # civis -> civil
        if p.endswith("ores") and len(p) >= 6:
            return p[:-2]                 # tratores -> trator
        if p.endswith("s") and not p.endswith("ss"):
            return p[:-1]                 # telhados -> telhado
        return p

    @staticmethod
    def calcular_similaridade_tokens(a, b):
        """
        6.3 & 6.4: Similaridade baseada em tokens (palavras) e fuzzy search.
        Garante que inversões de palavras (ex: 'mecanico manutencao') tenham boa correspondência
        e que plural/singular (ex: 'telhados' x 'telhado') casem entre si.
        """
        tokens_a = set(FuzzyEngine._singularizar(t) for t in str(a).split())
        tokens_b = set(FuzzyEngine._singularizar(t) for t in str(b).split())
        
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
