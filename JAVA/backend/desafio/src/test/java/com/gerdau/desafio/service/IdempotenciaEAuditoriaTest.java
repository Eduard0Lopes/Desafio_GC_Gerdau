package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.response.*;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.EventoAuditoria;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Idempotência (especificação, seção 7) e auditoria (seções 5 e 9):
 * - mesma chave + mesmo conteúdo → devolve o resultado original, sem duplicar;
 * - mesma chave + conteúdo diferente → conflito (409);
 * - criação e bloqueio geram eventos de auditoria.
 */
class IdempotenciaEAuditoriaTest {

    private CodigoCompletoService service;
    private CodigoCompletoRepository codigoCompletoRepository;
    private AuditoriaService auditoriaService;

    private static CriarCodigoCompletoRequest requisicao(String descricao) {
        return new CriarCodigoCompletoRequest("CIVIL", null, "200982674", null, null,
                null, descricao, "SV", null, null, null, "CRIAR", "403921");
    }

    private static PreviewResponse previewPermitido() {
        return new PreviewResponse("CIVIL_200982674_CS-000001_SV",
                new ComponentePreviewResponse("CIVIL", "Civil", false),
                new ComponentePreviewResponse("200982674", "Fornecedor", false),
                new ComponentePreviewResponse("CS-000001", "Servico", false),
                new ComponentePreviewResponse("SV", "SV - Serviço", false),
                "Civil | Fornecedor | Servico | SV", 10, false, false,
                "ORIGINAL", "PERMITIR", "ok", 0.0, List.of(), List.of());
    }

    @BeforeEach
    void configurar() {
        codigoCompletoRepository = Mockito.mock(CodigoCompletoRepository.class);
        ExcecaoRepository excecaoRepository = Mockito.mock(ExcecaoRepository.class);
        PreviewService previewService = Mockito.mock(PreviewService.class);
        CatalogoService catalogoService = Mockito.mock(CatalogoService.class);
        SimilaridadeService similaridadeService = Mockito.mock(SimilaridadeService.class);
        auditoriaService = Mockito.mock(AuditoriaService.class);

        com.gerdau.desafio.model.GrupoServico grupo = new com.gerdau.desafio.model.GrupoServico();
        grupo.setId("CIVIL");
        grupo.setNome("Civil");
        com.gerdau.desafio.model.CodigoFornecedor fornecedor = new com.gerdau.desafio.model.CodigoFornecedor();
        fornecedor.setId("200982674");
        fornecedor.setNome("Fornecedor");
        com.gerdau.desafio.model.CodigoServico servico = new com.gerdau.desafio.model.CodigoServico();
        servico.setId("CS-000001");
        servico.setDescricao("Servico");
        com.gerdau.desafio.model.UnidadeMedida unidade = new com.gerdau.desafio.model.UnidadeMedida();
        unidade.setId("SV");
        unidade.setSigla("SV");

        when(previewService.montarPreview(any())).thenReturn(previewPermitido());
        when(previewService.resolver(any(), eq(true))).thenReturn(new PreviewService.ComponentesResolvidos(
                grupo, false, fornecedor, false, servico, false, unidade, false));
        when(codigoCompletoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service = new CodigoCompletoService(codigoCompletoRepository, excecaoRepository, previewService,
                catalogoService, similaridadeService, auditoriaService, "enforce");
    }

    @Test
    void mesmaChaveEMesmoConteudoNaoDuplica() {
        CodigoCompleto existente = new CodigoCompleto();
        existente.setIdCompleto("CIVIL_200982674_CS-000001_SV");
        existente.setIdempotencyHash(CodigoCompletoService.hashDoConteudo(requisicao("Servico")));
        when(codigoCompletoRepository.findByIdempotencyKey("k1")).thenReturn(existente);

        CodigoCompletoResponse resposta = service.criar(requisicao("Servico"), "k1");

        assertEquals("CIVIL_200982674_CS-000001_SV", resposta.idCompleto());
        verify(codigoCompletoRepository, Mockito.never()).save(any());
    }

    @Test
    void mesmaChaveComConteudoDiferenteGeraConflito() {
        CodigoCompleto existente = new CodigoCompleto();
        existente.setIdempotencyHash(CodigoCompletoService.hashDoConteudo(requisicao("Servico")));
        when(codigoCompletoRepository.findByIdempotencyKey("k1")).thenReturn(existente);

        assertThrows(ConflictException.class, () -> service.criar(requisicao("Outro servico"), "k1"));
    }

    @Test
    void criacaoGeraEventoDeAuditoria() {
        when(codigoCompletoRepository.findByIdempotencyKey(anyString())).thenReturn(null);

        service.criar(requisicao("Servico novo"), null);

        ArgumentCaptor<String> acao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(acao.capture(), eq("CODIGO_COMPLETO"),
                any(), any(), any(), any(), any(), any());
        assertEquals(EventoAuditoria.ACAO_CRIACAO_CODIGO_COMPLETO, acao.getValue());
    }

    @Test
    void bloqueioGeraEventoDeAuditoria() {
        when(codigoCompletoRepository.findByIdempotencyKey(anyString())).thenReturn(null);
        PreviewResponse bloqueada = new PreviewResponse("X",
                new ComponentePreviewResponse("CIVIL", "Civil", false),
                new ComponentePreviewResponse("200982674", "Fornecedor", false),
                new ComponentePreviewResponse("CS-000001", "Servico", true),
                new ComponentePreviewResponse("SV", "SV", false),
                "snap", 10, false, false,
                "DUPLICIDADE", "REVISAR", "bloqueado", 0.95,
                List.of(new CandidatoSimilarResponse(1, "CS-000002", null, "Outro", 0.95, 0, 0, List.of(), "motivo")),
                List.of());
        // reconfigura o service com a prévia bloqueada
        PreviewService previewService = Mockito.mock(PreviewService.class);
        when(previewService.montarPreview(any())).thenReturn(bloqueada);
        service = new CodigoCompletoService(codigoCompletoRepository, Mockito.mock(ExcecaoRepository.class),
                previewService, Mockito.mock(CatalogoService.class), Mockito.mock(SimilaridadeService.class),
                auditoriaService, "enforce");

        assertThrows(ConflictException.class, () -> service.criar(requisicao("Servico repetido"), null));

        ArgumentCaptor<String> acao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(acao.capture(), eq("CODIGO_COMPLETO"),
                any(), any(), any(), any(), any(), any());
        assertEquals(EventoAuditoria.ACAO_BLOQUEIO, acao.getValue());
    }
}
