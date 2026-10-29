package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.request.VinculacaoLoteRequest;
import com.gerdau.desafio.dto.request.VinculacaoLoteRequest.ItemVinculacaoLote;
import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.ItemLoteResponse;
import com.gerdau.desafio.dto.response.LoteSimilaridadeResponse;
import com.gerdau.desafio.dto.response.LoteVinculacaoResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.model.CodigoCompleto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VinculacaoLoteTest {

    private CodigoCompletoService codigoCompletoService;
    private SimilaridadeService similaridadeService;
    private VinculacaoLoteService service;

    private final Map<String, ItemLoteResponse> analises = new HashMap<>();
    private final Set<String> recusados = new HashSet<>();
    private final List<CriarCodigoCompletoRequest> enviados = new ArrayList<>();

    @BeforeEach
    void configurar() {
        codigoCompletoService = Mockito.mock(CodigoCompletoService.class);
        similaridadeService = Mockito.mock(SimilaridadeService.class);
        service = new VinculacaoLoteService(codigoCompletoService, similaridadeService);
        analises.clear();
        recusados.clear();
        enviados.clear();

        when(similaridadeService.avaliarLote(anyList(), anyInt(), any())).thenAnswer(i -> {
            List<String> textos = i.getArgument(0);
            List<ItemLoteResponse> itens = new ArrayList<>();
            int disponiveis = 0;
            int aCriar = 0;
            int bloqueadas = 0;
            int linha = 0;
            for (String texto : textos) {
                linha++;
                ItemLoteResponse base = analises.getOrDefault(texto,
                        new ItemLoteResponse(linha, texto, "CRIAR", "OK", "novo", "PERMITIR", 0.0, null));
                itens.add(new ItemLoteResponse(linha, texto, base.classificacao(), base.situacao(),
                        base.motivo(), base.decisao(), base.melhorScore(), base.similar()));
                switch (base.classificacao()) {
                    case "DISPONIVEL" -> disponiveis++;
                    case "CRIAR" -> aCriar++;
                    default -> bloqueadas++;
                }
            }
            return new LoteSimilaridadeResponse("CS", itens.size(), disponiveis, aCriar, bloqueadas, itens);
        });

        when(codigoCompletoService.criar(any(), any())).thenAnswer(i -> {
            CriarCodigoCompletoRequest req = i.getArgument(0);
            enviados.add(req);
            String identidade = req.csId() != null ? req.csId() : req.csNovaDescricao();
            if (recusados.contains(identidade)) {
                throw new BusinessException("recusado pelo servidor");
            }
            String csId = req.csId() != null ? req.csId() : "CS-NOVO";
            boolean validacao = PreviewService.ACAO_ENVIAR_VALIDACAO.equals(req.acao());
            return new CodigoCompletoService.ResultadoCriacao(
                    new CodigoCompletoResponse("reg-" + csId,
                            req.gsId() + "_" + req.cfId() + "_" + csId + "_" + req.umId(),
                            req.gsId(), req.cfId(), csId, req.umId(), "snapshot", "APLICACAO", List.of(),
                            validacao ? CodigoCompleto.STATUS_PENDENTE : CodigoCompleto.STATUS_ATIVO,
                            "PERMITIR", 0.0, req.justificativa(), req.usuarioCriacaoId(),
                            null, null, null),
                    false);
        });
    }

    private void classificar(String texto, String classificacao, String situacao,
                             String decisao, CandidatoSimilarResponse similar) {
        analises.put(texto, new ItemLoteResponse(0, texto, classificacao, situacao,
                "motivo " + situacao.toLowerCase(), decisao, similar == null ? 0.0 : similar.score(), similar));
    }

    private static CandidatoSimilarResponse similar(String id, double score) {
        return new CandidatoSimilarResponse(1, id, null, "EXISTENTE " + id, score,
                score, score, List.of(), "parecido", false, null);
    }

    private LoteVinculacaoResponse vincular(ItemVinculacaoLote... itens) {
        return service.vincularEmLote(new VinculacaoLoteRequest("CIVIL", "1", "UN",
                List.of(itens), "ana"), "lote-1");
    }

    @Test
    void codigoExistenteEscolhidoEhReutilizadoENadaEhCriado() {
        LoteVinculacaoResponse resposta = vincular(new ItemVinculacaoLote("CS-EXIST", null));

        assertEquals(1, resposta.reutilizados());
        assertEquals(0, resposta.criados());
        assertEquals(1, resposta.vinculados());
        assertEquals("CS-EXIST", enviados.get(0).csId());
        assertNull(enviados.get(0).csNovaDescricao());
        assertEquals(PreviewService.ACAO_CRIAR, enviados.get(0).acao());
    }

    @Test
    void codigoNovoSemSimilaridadeEhCriadoEVinculado() {
        classificar("SV MONTAGEM PIPA", "CRIAR", "OK", "PERMITIR", null);

        LoteVinculacaoResponse resposta = vincular(new ItemVinculacaoLote(null, "SV MONTAGEM PIPA"));

        assertEquals(1, resposta.criados());
        assertEquals(1, resposta.vinculados());
        assertEquals("SV MONTAGEM PIPA", enviados.get(0).csNovaDescricao());
        assertEquals(PreviewService.ACAO_CRIAR, enviados.get(0).acao());
    }

    @Test
    void codigoNovoComAlertaDeSimilaridadeVaiParaAGovernanca() {
        classificar("SV REPARO COBERTURA", "CRIAR", "ALERTA", "ALERTAR", similar("CS-0002", 0.75));

        LoteVinculacaoResponse resposta = vincular(new ItemVinculacaoLote(null, "SV REPARO COBERTURA"));

        assertEquals(1, resposta.aguardandoValidacao());
        assertEquals(0, resposta.vinculados());
        assertEquals("ALERTAR", resposta.itens().get(0).decisao());
        assertEquals(PreviewService.ACAO_ENVIAR_VALIDACAO, enviados.get(0).acao());
        assertNotNull(enviados.get(0).justificativa());
    }

    @Test
    void duplicidadeProvavelReutilizaOMelhorMatch() {
        classificar("SV EQUIPE TELHADO 2", "DISPONIVEL", "PARECIDO", "REVISAR", similar("CS-0001", 0.93));

        LoteVinculacaoResponse resposta = vincular(new ItemVinculacaoLote(null, "SV EQUIPE TELHADO 2"));

        assertEquals(1, resposta.reutilizados());
        assertEquals("CS-0001", resposta.itens().get(0).csId());
        assertEquals("CS-0001", enviados.get(0).csId());
    }

    @Test
    void linhaBloqueadaNaoChegaACriar() {
        classificar("SV REPETIDO", "BLOQUEADA", "REPETIDA", null, null);

        LoteVinculacaoResponse resposta = vincular(new ItemVinculacaoLote(null, "SV REPETIDO"));

        assertEquals("BLOQUEADA", resposta.itens().get(0).classificacao());
        assertEquals(0, resposta.vinculados());
        verify(codigoCompletoService, never()).criar(any(), any());
    }

    @Test
    void umaFalhaNaoDerrubaAsDemais() {
        classificar("SV BOM", "CRIAR", "OK", "PERMITIR", null);
        classificar("SV RUIM", "CRIAR", "OK", "PERMITIR", null);
        recusados.add("SV RUIM");

        LoteVinculacaoResponse resposta = vincular(
                new ItemVinculacaoLote(null, "SV BOM"),
                new ItemVinculacaoLote(null, "SV RUIM"));

        assertEquals(1, resposta.vinculados());
        assertEquals("FALHA", resposta.itens().get(1).classificacao());
    }

    @Test
    void linhasRepetidasNaoSaoBarradasEViramVinculosIndependentes() {
        classificar("SV MONTAGEM PIPA", "CRIAR", "OK", "PERMITIR", null);

        LoteVinculacaoResponse resposta = vincular(
                new ItemVinculacaoLote(null, "SV MONTAGEM PIPA"),
                new ItemVinculacaoLote(null, "SV MONTAGEM PIPA"),
                new ItemVinculacaoLote("CS-EXIST", null),
                new ItemVinculacaoLote("CS-EXIST", null));

        assertEquals(2, resposta.criados());
        assertEquals(2, resposta.reutilizados());
        assertEquals(4, resposta.vinculados());
    }
}
