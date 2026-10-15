package com.gerdau.desafio.similarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valores de referência calculados com difflib.SequenceMatcher (Python) —
 * a mesma base usada pelo motor do Time de IA (IA/src/fuzzy_engine.py).
 * Casos espelhados em IA/src/validation/paridade.py.
 */
class SimilarityEngineTest {

    private static final double TOLERANCIA = 1e-5;

    @Test
    void textosIguaisTemScoreUm() {
        assertEquals(1.0, SimilarityEngine.score("manutencao mecanica normal", "manutencao mecanica normal"), TOLERANCIA);
    }

    @Test
    void palavrasInvertidasTemTokensIguaisECaracteresDiferentes() {
        assertEquals(1.0, SimilarityEngine.jaccard(
                SimilarityEngine.tokens("mecanico manutencao"), SimilarityEngine.tokens("manutencao mecanico")), TOLERANCIA);
        assertEquals(0.526316, SimilarityEngine.razaoCaracteres("mecanico manutencao", "manutencao mecanico"), TOLERANCIA);
        assertEquals(0.715789, SimilarityEngine.score("mecanico manutencao", "manutencao mecanico"), TOLERANCIA);
    }

    @Test
    void servicosParecidosFicamAbaixoDeDuplicidade() {
        assertEquals(0.56, SimilarityEngine.score("mecanico manutencao hora normal", "manutencao mecanica normal"), TOLERANCIA);
    }

    @Test
    void servicosDiferentesTemScoreBaixo() {
        assertEquals(0.15, SimilarityEngine.score("pintura predial", "limpeza de vidros"), TOLERANCIA);
    }

    @Test
    void textoVazioTemScoreZero() {
        assertEquals(0.0, SimilarityEngine.score("", "manutencao"), TOLERANCIA);
        assertEquals(0.0, SimilarityEngine.score("manutencao", ""), TOLERANCIA);
    }

    @Test
    void razaoDeCaracteresSegueOAlgoritmoDoDifflib() {
        assertEquals(0.75, SimilarityEngine.razaoCaracteres("abcd", "bcde"), TOLERANCIA);
    }

    @Test
    void numerosDiferentesLimitamOScoreMasNaoAlteramOScoreBase() {
        String a = "sv eletroduto pvc rigido roscavel 1 2";
        String b = "sv eletroduto pvc rigido roscavel 2 1 2";
        assertEquals(java.util.List.of("1", "2"), SimilarityEngine.numeros(a));
        assertTrue(SimilarityEngine.score(a, b) > 0.88, "o score base ficaria acima do limiar de duplicidade");
        assertEquals(SimilarityEngine.TETO_NUMEROS_DIFERENTES, SimilarityEngine.scoreComRegraNumerica(a, b), TOLERANCIA);
        assertEquals(SimilarityEngine.score(a, a), SimilarityEngine.scoreComRegraNumerica(a, a), TOLERANCIA);
    }
}
