package br.com.gerdau.servicos.api.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimilarityPolicyTest {

    private final SimilarityPolicy policy = new SimilarityPolicy(0.75, 0.90);

    @Test
    void classificaPelasFaixasDoDocumento() {
        assertEquals(DecisaoSimilaridade.REVISAR, policy.decide(0.90));
        assertEquals(DecisaoSimilaridade.ALERTAR, policy.decide(0.89));
        assertEquals(DecisaoSimilaridade.ALERTAR, policy.decide(0.75));
        assertEquals(DecisaoSimilaridade.PERMITIR, policy.decide(0.74));
    }

    @Test
    void rejeitaLimiaresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new SimilarityPolicy(0.9, 0.75));
        assertThrows(IllegalArgumentException.class, () -> new SimilarityPolicy(0.0, 0.9));
        assertThrows(IllegalArgumentException.class, () -> new SimilarityPolicy(0.5, 1.1));
    }
}
