class RankingPolicy:
    @staticmethod
    def aplicar_politica_ranking(score):
        """
        Política de Ranking Calibrada (Gerdau):
        - score >= 0.88 : provável duplicidade / bloquear ou exigir justificativa
        - score entre 0.70 e 0.87 : apresentar alerta e encaminhar para decisão
        - score abaixo di 0.70 : permitir continuidade, mantendo a validação de negócio
        """
        if score >= 0.88:
            return "DUPLICIDADE", "Provável duplicidade - Bloquear ou exigir justificativa"
        elif score >= 0.70:
            return "ALERTA", "Alerta de similaridade - Encaminhar para decisão"
        else:
            return "ORIGINAL", "Permitir continuidade - Novo registro com validação"