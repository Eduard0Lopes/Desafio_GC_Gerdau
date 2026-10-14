package com.gerdau.desafio.service.similaridade;

/**
 * Espelho de {@code IA/src/ranking_policy.py}. Altere os limiares nos dois lugares.
 */
public final class RankingPolicy {

    public static final double LIMIAR_DUPLICIDADE = 0.88;
    public static final double LIMIAR_ALERTA = 0.70;

    private RankingPolicy() {
    }

    public static StatusSimilaridade classificar(double score) {
        if (score >= LIMIAR_DUPLICIDADE) {
            return StatusSimilaridade.DUPLICIDADE;
        }
        if (score >= LIMIAR_ALERTA) {
            return StatusSimilaridade.ALERTA;
        }
        return StatusSimilaridade.ORIGINAL;
    }
}
