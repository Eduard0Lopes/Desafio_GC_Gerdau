package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.dto.response.LoteSimilaridadeResponse;
import com.gerdau.desafio.search.SearchClient;
import com.gerdau.desafio.similarity.RankingPolicy;
import com.gerdau.desafio.util.TextoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SimilaridadeLoteTest {

    private SearchClient searchClient;
    private SimilaridadeService service;

    @BeforeEach
    void configurar() {
        searchClient = Mockito.mock(SearchClient.class);
        service = new SimilaridadeService(searchClient, new RankingPolicy(0.88, 0.70), 0.30);
    }

    private void baseContemPara(String consulta, CandidatoSimilarResponse... candidatos) {
        when(searchClient.buscarSimilares(eq(consulta), anyInt(), anyDouble(), any()))
                .thenReturn(List.of(candidatos));
    }

    private void baseVazia() {
        when(searchClient.buscarSimilares(anyString(), anyInt(), anyDouble(), any()))
                .thenReturn(List.of());
    }

    private static CandidatoSimilarResponse similar(String id, String descricao, double score) {
        return new CandidatoSimilarResponse(1, id, null, descricao, score, score, score,
                List.of(), "parecido", false, null);
    }

    @Test
    void separaDisponiveisDeCriarERepetidas() {
        baseContemPara("SV EQUIPE TELHADO", similar("CS-0001", "SV EQUIPE TELHADO", 1.0));
        baseContemPara("LOCAÇÃO GUINDASTE");

        LoteSimilaridadeResponse lote = service.avaliarLote(
                List.of("SV EQUIPE TELHADO", "LOCAÇÃO GUINDASTE", "sv equipe telhado"), 3, "CS");

        assertEquals(3, lote.total());
        assertEquals(1, lote.disponiveis());
        assertEquals(1, lote.aCriar());
        assertEquals(1, lote.bloqueadas());
        assertEquals("DISPONIVEL", lote.itens().get(0).classificacao());
        assertEquals("JA_EXISTE", lote.itens().get(0).situacao());
        assertEquals("CRIAR", lote.itens().get(1).classificacao());
        assertEquals("OK", lote.itens().get(1).situacao());
        assertEquals("BLOQUEADA", lote.itens().get(2).classificacao());
        assertEquals("REPETIDA", lote.itens().get(2).situacao());
    }

    @Test
    void duplicidadeProvavelContaComoDisponivelParaReutilizar() {
        baseContemPara("SV EQUIPE TELHADO 2", similar("CS-0001", "SV EQUIPE TELHADO", 0.93));

        LoteSimilaridadeResponse lote = service.avaliarLote(List.of("SV EQUIPE TELHADO 2"), 3, "CS");

        assertEquals(1, lote.disponiveis());
        assertEquals(0, lote.aCriar());
        assertEquals("DISPONIVEL", lote.itens().get(0).classificacao());
        assertEquals("PARECIDO", lote.itens().get(0).situacao());
        assertEquals("CS-0001", lote.itens().get(0).similar().id());
    }

    @Test
    void alertaDeSimilaridadeAindaPrecisaSerCriado() {
        baseContemPara("LOCAÇÃO ANDAIME", similar("CS-0002", "LOCAÇÃO DE ANDAIME", 0.75));

        LoteSimilaridadeResponse lote = service.avaliarLote(List.of("LOCAÇÃO ANDAIME"), 3, "CS");

        assertEquals(0, lote.disponiveis());
        assertEquals(1, lote.aCriar());
        assertEquals("CRIAR", lote.itens().get(0).classificacao());
        assertEquals("ALERTA", lote.itens().get(0).situacao());
    }

    @Test
    void linhasInvalidasOuAcimaDoLimiteSapFicamBloqueadas() {
        baseVazia();
        String acimaDoSap = "X".repeat(TextoUtil.LIMITE_SAP + 1);

        LoteSimilaridadeResponse lote = service.avaliarLote(List.of("", acimaDoSap), 3, "CS");

        assertEquals(2, lote.bloqueadas());
        assertEquals("VAZIA", lote.itens().get(0).situacao());
        assertEquals("LIMITE_SAP", lote.itens().get(1).situacao());
    }
}
