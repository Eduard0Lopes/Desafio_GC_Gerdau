"""Política de ranking calibrada (espelhada em service/similaridade/RankingPolicy.java).

- score >= 0.88        : provável duplicidade -> bloquear ou exigir justificativa
- 0.70 <= score < 0.88 : alerta -> encaminhar para decisão
- score < 0.70         : permitir continuidade, mantendo a validação de negócio
"""
LIMIAR_DUPLICIDADE = 0.88
LIMIAR_ALERTA = 0.70

DUPLICIDADE = "DUPLICIDADE"
ALERTA = "ALERTA"
ORIGINAL = "ORIGINAL"

ACOES = {
    DUPLICIDADE: "Provável duplicidade - Bloquear ou exigir justificativa",
    ALERTA: "Alerta de similaridade - Encaminhar para decisão",
    ORIGINAL: "Permitir continuidade - Novo registro com validação",
}


def classificar(score: float) -> tuple[str, str]:
    """Retorna (status, ação recomendada) para o score informado."""
    if score >= LIMIAR_DUPLICIDADE:
        status = DUPLICIDADE
    elif score >= LIMIAR_ALERTA:
        status = ALERTA
    else:
        status = ORIGINAL
    return status, ACOES[status]
