package com.gerdau.desafio.similarity;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingEngineTest {

    private final List<CatalogoItem> catalogo = List.of(
            RankingEngine.prepararItem("CS-000003", "4521", List.of("MEC.MANUT.NORM"), "Manutenção mecânica normal", null),
            RankingEngine.prepararItem("CS-000001", "7001", List.of(), "MANUTENCAO MECANICA NORMAL", null),
            RankingEngine.prepararItem("CS-000002", "7002", List.of(), "Pintura predial", null),
            RankingEngine.prepararItem("CS-000004", "7003", List.of(), "Mecânico manutenção", null));

    @Test
    void descricaoIgualAposNormalizacaoTemScoreUmEEmpateDesempatadoPeloId() {
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("Manutenção Mecânica Normal!", catalogo, 5, 0.30);

        assertEquals("CS-000001", r.get(0).id()); // empate de score 1.0: menor id primeiro
        assertEquals("CS-000003", r.get(1).id());
        assertEquals(1.0, r.get(0).score());
        assertEquals(1, r.get(0).posicao());
        assertTrue(r.get(0).motivo().startsWith("Mesma descrição"));
    }

    @Test
    void respeitaLimiteEScoreMinimo() {
        assertEquals(2, RankingEngine.ranquear("manutencao mecanica normal", catalogo, 2, 0.30).size());
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("manutencao mecanica normal", catalogo, 10, 0.30);
        assertTrue(r.stream().noneMatch(c -> c.id().equals("CS-000002")), "pintura predial está abaixo do score mínimo");
    }

    @Test
    void codigoLegadoIgualAoInformadoTemScoreUm() {
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("4521", catalogo, 3, 0.30);
        assertEquals("CS-000003", r.get(0).id());
        assertEquals(1.0, r.get(0).score());
    }

    @Test
    void ordemEReproduzivel() {
        List<CandidatoSimilarResponse> a = RankingEngine.ranquear("mecanico manutencao normal", catalogo, 5, 0.0);
        List<CandidatoSimilarResponse> b = RankingEngine.ranquear("mecanico manutencao normal", catalogo, 5, 0.0);
        assertEquals(a, b);
        for (int i = 1; i < a.size(); i++) {
            assertTrue(a.get(i - 1).score() >= a.get(i).score());
        }
    }

    @Test
    void medidasDiferentesNaoViramDuplicidade() {
        List<CatalogoItem> eletrodutos = List.of(
                RankingEngine.prepararItem("CS-001232", null, List.of(), "SV ELETRODUTO PVC 1/2", null),
                RankingEngine.prepararItem("CS-001234", null, List.of(), "SV ELETRODUTO PVC 2 1/2", null));
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("SV ELETRODUTO PVC 1 1/2", eletrodutos, 5, 0.30);
        assertTrue(r.get(0).score() <= SimilarityEngine.TETO_NUMEROS_DIFERENTES);
        assertTrue(r.get(0).motivo().contains("limitado a 85%"));
    }

    @Test
    void chaveDeComparacaoERenormalizada() {
        // base legada com "nomeNormalizado" só em minúsculas (com acento) não pode quebrar o match
        CatalogoItem item = RankingEngine.prepararItem("GS-01", null, null, "Eletromecânica", "eletromecânica");
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("Eletromecanica", List.of(item), 5, 0.30);
        assertEquals(1.0, r.get(0).score());
    }
}
