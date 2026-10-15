package com.gerdau.desafio.similarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RankingPolicyTest {

    private final RankingPolicy policy = new RankingPolicy(0.88, 0.70);

    @Test
    void classificaPelasFaixasDeScore() {
        assertEquals("DUPLICIDADE", policy.aplicar(0.88).status());
        assertEquals("REVISAR", policy.aplicar(1.0).decisao());
        assertEquals("ALERTA", policy.aplicar(0.87).status());
        assertEquals("ALERTAR", policy.aplicar(0.70).decisao());
        assertEquals("ORIGINAL", policy.aplicar(0.699).status());
        assertEquals("PERMITIR", policy.aplicar(0.0).decisao());
    }

    @Test
    void rejeitaLimiaresInvertidos() {
        assertThrows(IllegalArgumentException.class, () -> new RankingPolicy(0.5, 0.9));
    }
}
