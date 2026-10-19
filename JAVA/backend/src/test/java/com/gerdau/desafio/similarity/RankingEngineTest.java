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
    void palavraEspecificaComErroDeEscritaGeraAlerta() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "Telhado metálico galvanizado", null));
        for (String variante : List.of("telado", "telhadoss", "TELHADO!")) {
            List<CandidatoSimilarResponse> r = RankingEngine.ranquear(variante, itens, 5, 0.30);
            assertEquals(1, r.size(), variante);
            // alerta (>= 0.70), sem virar duplicidade automática (< 1.0)
            assertTrue(r.get(0).score() >= 0.70, variante + ": " + r.get(0).score());
            assertTrue(r.get(0).score() < 1.0, variante);
            assertTrue(r.get(0).motivo().contains("palavra"), variante);
        }
    }

    @Test
    void descricaoLongaNaoDiluiAPalavraQueCasa() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(),
                        "Troca de revestimento em telhado de galpão industrial", null));
        for (String variante : List.of("telado", "telhadoss")) {
            List<CandidatoSimilarResponse> r = RankingEngine.ranquear(variante, itens, 5, 0.30);
            assertTrue(r.get(0).score() >= 0.70, variante + ": " + r.get(0).score());
        }
    }

    @Test
    void palavraGenericaNaoDisparaAlertaSozinha() {
        List<CatalogoItem> itens = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            itens.add(RankingEngine.prepararItem("CS-" + i, null, List.of(), "Manutencao equipamento " + i, null));
        }
        for (CandidatoSimilarResponse c : RankingEngine.ranquear("manutencao", itens, 5, 0.30)) {
            assertTrue(c.score() < 0.70, c.motivo());
        }
    }

    @Test
    void numeroComEspacoNaoEscapaDaDuplicidade() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "telhados", null));
        for (String variante : List.of("telhado 2", "telhado 10", "telhado2", "telhado 2!")) {
            List<CandidatoSimilarResponse> r = RankingEngine.ranquear(variante, itens, 5, 0.30);
            assertEquals(1, r.size(), variante);
            assertEquals(1.0, r.get(0).score(), variante);
            assertTrue(r.get(0).motivo().contains("Essencialmente"), variante);
        }
    }

    @Test
    void medidasDiferentesNaoViramDuplicidadeComOuSemEspaco() {
        // "telhado 2" e "telhado2" contra "telhado 3" seguem a regra de medidas
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "telhado 3", null));
        for (String variante : List.of("telhado 2", "telhado2")) {
            for (CandidatoSimilarResponse c : RankingEngine.ranquear(variante, itens, 5, 0.30)) {
                assertTrue(c.score() < 0.88, variante + ": " + c.motivo());
            }
        }
    }

    @Test
    void repeticaoDeLetrasNaoEscapaDaDuplicidade() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "telhados", null));
        for (String variante : List.of("telhadosss", "telhadossss", "TELHADO", "telhadoo", "telhadoa", "tehado")) {
            List<CandidatoSimilarResponse> r = RankingEngine.ranquear(variante, itens, 5, 0.30);
            assertEquals(1, r.size(), variante);
            assertEquals(1.0, r.get(0).score(), variante);
            assertTrue(r.get(0).motivo().contains("Essencialmente"), variante);
        }
    }

    @Test
    void medidasCurtasDiferentesNaoViramDuplicidade() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "Pintura m2", null));
        for (CandidatoSimilarResponse c : RankingEngine.ranquear("Pintura m3", itens, 5, 0.30)) {
            assertTrue(c.score() < 0.88, c.motivo());
        }
    }

    @Test
    void palavraRealAMaisContinuaPassando() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "telhados", null));
        for (CandidatoSimilarResponse c : RankingEngine.ranquear("Telhados de fibra", itens, 5, 0.30)) {
            assertTrue(c.score() < 0.88, c.motivo());
        }
    }

    @Test
    void nomeEssencialmenteIgualViraDuplicidadeMesmoComNumeracaoOuSigla() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "teste", null));
        for (String variante : List.of("teste1", "SV - teste", "TESTES")) {
            List<CandidatoSimilarResponse> r = RankingEngine.ranquear(variante, itens, 5, 0.30);
            assertEquals(1, r.size(), variante);
            assertEquals(1.0, r.get(0).score(), variante);
            assertTrue(r.get(0).motivo().contains("Essencialmente"), variante);
        }
    }

    @Test
    void palavrasReaisAMaisNaoSaoDuplicidade() {
        List<CatalogoItem> itens = List.of(
                RankingEngine.prepararItem("CS-1", null, List.of(), "teste", null));
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("Teste de algo", itens, 5, 0.30);
        // pode aparecer como sugestão, mas nunca como duplicidade (score < 0.88)
        for (CandidatoSimilarResponse c : r) {
            assertTrue(c.score() < 0.88, c.motivo());
        }
    }

    @Test
    void descricaoIgualAposNormalizacaoTemScoreUmEEmpateDesempatadoPeloId() {
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("Manutenção Mecânica Normal!", catalogo, 5, 0.30);

        assertEquals("CS-000001", r.get(0).id()); 
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
        
        CatalogoItem item = RankingEngine.prepararItem("GS-01", null, null, "Eletromecânica", "eletromecânica");
        List<CandidatoSimilarResponse> r = RankingEngine.ranquear("Eletromecanica", List.of(item), 5, 0.30);
        assertEquals(1.0, r.get(0).score());
    }

    @Test
    void pluralESingularEncontramOMesmoItemComScoreAlto() {
        CatalogoItem item = RankingEngine.prepararItem("CS-009001", null, List.of(), "Telhado metálico galvanizado", null);

        List<CandidatoSimilarResponse> plural = RankingEngine.ranquear("Telhados metálicos galvanizados", List.of(item), 5, 0.30);
        assertEquals("CS-009001", plural.get(0).id());
        assertEquals(1.0, plural.get(0).scoreTokens());
        assertTrue(plural.get(0).score() > 0.9, "busca no plural deve pegar o item no singular");

        List<CandidatoSimilarResponse> singular = RankingEngine.ranquear("Telhado metálico galvanizado", List.of(item), 5, 0.30);
        assertEquals("CS-009001", singular.get(0).id());
        assertEquals(1.0, singular.get(0).score());
    }
}
