class RankingPolicy:
    @staticmethod
    def aplicar_politica_ranking(score):
        """
        6.7 Política Inicial de Ranking:
        - score >= 0.90 : provável duplicidade / bloquear ou exigir justificativa
        - score entre 0.75 e 0.89 : apresentar alerta e encaminhar para decisão
        - score abaixo de 0.75 : permitir continuidade, mantendo a validação de negócio
        """
        if score >= 0.90:
            return "PROVAVEL_DUPLICIDADE", "Bloquear ou exigir justificativa"
        elif score >= 0.75:
            return "ALERTA_SIMILARIDADE", "Apresentar alerta e encaminhar para decisão"
        else:
            return "NOVO_REGISTRO", "Permitir continuidade com validação de negócio"