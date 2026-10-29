package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.response.ComponentePreviewResponse;
import com.gerdau.desafio.dto.response.PreviewResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PedidosGovernancaTest {

    private CodigoCompletoRepository codigoCompletoRepository;
    private PreviewService previewService;
    private CatalogoService catalogoService;
    private SimilaridadeService similaridadeService;
    private CodigoCompletoService service;
    private org.mockito.MockedStatic<AuditoriaService> identidade;

    @BeforeEach
    void configurar() {
        identidade = Mockito.mockStatic(AuditoriaService.class);
        identidade.when(AuditoriaService::usuarioAtual).thenReturn("403921");
        identidade.when(AuditoriaService::papelAtual).thenReturn("USUARIO");
        identidade.when(AuditoriaService::ehAdmin).thenReturn(false);

        codigoCompletoRepository = Mockito.mock(CodigoCompletoRepository.class);
        previewService = Mockito.mock(PreviewService.class);
        catalogoService = Mockito.mock(CatalogoService.class);
        similaridadeService = Mockito.mock(SimilaridadeService.class);
        when(similaridadeService.consultar(any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new com.gerdau.desafio.dto.response.SimilaridadeResponse(
                        "x", "x", 1, false, "ORIGINAL", "PERMITIR", "ok", java.util.Map.of(), 0, List.of()));
        service = new CodigoCompletoService(codigoCompletoRepository, Mockito.mock(ExcecaoRepository.class),
                previewService, catalogoService, similaridadeService,
                Mockito.mock(AuditoriaService.class), "enforce");
        when(codigoCompletoRepository.save(any(CodigoCompleto.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void encerrar() {
        identidade.close();
    }

    private static PreviewResponse previewCom(boolean jaExiste, String statusExistente) {
        return new PreviewResponse("GOVERNANCA_1440531_CS-000010_UN",
                new ComponentePreviewResponse("GOVERNANCA", "Governança", false),
                new ComponentePreviewResponse("1440531", "Fornecedor", false),
                new ComponentePreviewResponse("CS-000010", "Serviço", false),
                new ComponentePreviewResponse("UN", "UN - Unidade", false),
                "snap", 10, false, jaExiste, statusExistente,
                "ORIGINAL", "PERMITIR", "ok", 0.0, List.of(), List.of());
    }

    private static CriarCodigoCompletoRequest pedido() {
        return new CriarCodigoCompletoRequest("GOVERNANCA", null, "1440531", null, null,
                "CS-000010", null, "UN", null, null, null, "CRIAR", "403921");
    }

    private static CodigoCompleto pendente() {
        CodigoCompleto model = new CodigoCompleto();
        model.setId("GOVERNANCA_1440531_CS-000010_UN");
        model.setIdCompleto("GOVERNANCA_1440531_CS-000010_UN");
        model.setGsId("GOVERNANCA");
        model.setCfId("1440531");
        model.setCsId("CS-000010");
        model.setUmId("UN");
        model.setUsuarioCriacaoId("403921");
        model.setStatus(CodigoCompleto.STATUS_PENDENTE);
        model.setDecisaoSimilaridade("ALERTAR");
        model.setDescricaoSnapshot("Governança | Fornecedor | Serviço | UN");
        return model;
    }

    @Test
    void composicaoComPedidoPendenteTambemCriaUmNovoVinculo() {
        when(previewService.montarPreview(any())).thenReturn(previewCom(true, CodigoCompleto.STATUS_PENDENTE));
        when(previewService.resolver(any(), anyBoolean())).thenReturn(new PreviewService.ComponentesResolvidos(
                new GrupoServico(), false, new CodigoFornecedor(), false,
                new CodigoServico(), false, new UnidadeMedida(), false));
        when(previewService.montarSnapshot(any())).thenReturn("snap");

        assertDoesNotThrow(() -> service.criar(pedido(), null));

        verify(codigoCompletoRepository).save(any(CodigoCompleto.class));
    }

    @Test
    void composicaoAtivaTambemCriaUmNovoVinculo() {
        when(previewService.montarPreview(any())).thenReturn(previewCom(true, CodigoCompleto.STATUS_ATIVO));
        when(previewService.resolver(any(), anyBoolean())).thenReturn(new PreviewService.ComponentesResolvidos(
                new GrupoServico(), false, new CodigoFornecedor(), false,
                new CodigoServico(), false, new UnidadeMedida(), false));
        when(previewService.montarSnapshot(any())).thenReturn("snap");

        assertDoesNotThrow(() -> service.criar(pedido(), null));

        verify(codigoCompletoRepository).save(any(CodigoCompleto.class));
    }

    @Test
    void cadaVinculoRecebeUmIdProprioQuandoAComposicaoJaTemRegistros() {
        GrupoServico grupo = new GrupoServico();
        grupo.setId("GOVERNANCA");
        grupo.setNome("Governança");
        CodigoFornecedor fornecedor = new CodigoFornecedor();
        fornecedor.setId("1440531");
        fornecedor.setNome("Fornecedor");
        CodigoServico servico = new CodigoServico();
        servico.setId("CS-000010");
        servico.setDescricao("Serviço");
        UnidadeMedida unidade = new UnidadeMedida();
        unidade.setId("UN");
        unidade.setSigla("UN");

        when(previewService.montarPreview(any())).thenReturn(previewCom(true, CodigoCompleto.STATUS_ATIVO));
        when(previewService.resolver(any(), anyBoolean())).thenReturn(new PreviewService.ComponentesResolvidos(
                grupo, false, fornecedor, false, servico, false, unidade, false));
        when(previewService.montarSnapshot(any())).thenReturn("snap");
        when(codigoCompletoRepository.findById("GOVERNANCA_1440531_CS-000010_UN"))
                .thenReturn(Optional.of(pendente()));

        var resultado = service.criar(pedido(), null);

        ArgumentCaptor<CodigoCompleto> salvo = ArgumentCaptor.forClass(CodigoCompleto.class);
        verify(codigoCompletoRepository).save(salvo.capture());
        assertEquals("GOVERNANCA_1440531_CS-000010_UN#2", salvo.getValue().getId());
        assertEquals("GOVERNANCA_1440531_CS-000010_UN", salvo.getValue().getIdCompleto());
        assertFalse(resultado.requisicaoRepetida());
    }

    @Test
    void composicaoRejeitadaPodeSerReenviada() {
        GrupoServico grupo = new GrupoServico();
        grupo.setId("GOVERNANCA");
        grupo.setNome("Governança");
        CodigoFornecedor fornecedor = new CodigoFornecedor();
        fornecedor.setId("1440531");
        fornecedor.setNome("Fornecedor");
        CodigoServico servico = new CodigoServico();
        servico.setId("CS-000010");
        servico.setDescricao("Serviço");
        UnidadeMedida unidade = new UnidadeMedida();
        unidade.setId("UN");
        unidade.setSigla("UN");

        when(previewService.montarPreview(any())).thenReturn(previewCom(false, CodigoCompleto.STATUS_REJEITADO));
        when(previewService.resolver(any(), anyBoolean())).thenReturn(new PreviewService.ComponentesResolvidos(
                grupo, false, fornecedor, false, servico, false, unidade, false));
        when(previewService.montarSnapshot(any())).thenReturn("snap");

        assertDoesNotThrow(() -> service.criar(pedido(), null));
        verify(codigoCompletoRepository).save(any(CodigoCompleto.class));
    }

    @Test
    void editarPedidoPendenteAlteraDescricaoEJustificativa() {
        CodigoCompleto model = pendente();
        when(codigoCompletoRepository.findById(model.getId())).thenReturn(Optional.of(model));
        when(previewService.resolver(any(), eq(false))).thenReturn(new PreviewService.ComponentesResolvidos(
                new GrupoServico(), false, new CodigoFornecedor(), false,
                new CodigoServico(), false, new UnidadeMedida(), false));
        when(previewService.montarSnapshot(any())).thenReturn("Governança | Fornecedor | Serviço nova | UN");

        service.atualizarPedido(model.getId(), "Serviço nova", "descrição ajustada após revisão");

        verify(catalogoService).atualizarDescricaoServico("CS-000010", "Serviço nova");
        assertEquals("descrição ajustada após revisão", model.getJustificativa());
        assertEquals("Governança | Fornecedor | Serviço nova | UN", model.getDescricaoSnapshot());
    }

    @Test
    void editarPedidoSemNadaParaAlterarEBloqueado() {
        CodigoCompleto model = pendente();
        when(codigoCompletoRepository.findById(model.getId())).thenReturn(Optional.of(model));

        assertThrows(BusinessException.class,
                () -> service.atualizarPedido(model.getId(), "  ", null));
    }

    @Test
    void cancelarPedidoPendenteLiberaParaNovoEnvio() {
        CodigoCompleto model = pendente();
        when(codigoCompletoRepository.findById(model.getId())).thenReturn(Optional.of(model));

        var resposta = service.cancelarPedido(model.getId());

        assertEquals(CodigoCompleto.STATUS_CANCELADO, resposta.status());
        assertEquals(CodigoCompleto.STATUS_CANCELADO, model.getStatus());
    }

    @Test
    void pedidoQueNaoEstaPendenteNaoPodeSerEditadoNemCancelado() {
        CodigoCompleto model = pendente();
        model.setStatus(CodigoCompleto.STATUS_ATIVO);
        when(codigoCompletoRepository.findById(model.getId())).thenReturn(Optional.of(model));

        assertThrows(BusinessException.class, () -> service.cancelarPedido(model.getId()));
        assertThrows(BusinessException.class, () -> service.atualizarPedido(model.getId(), "nova", null));
    }

    @Test
    void somenteODonoOuAdminPodeEditarECancelar() {
        CodigoCompleto model = pendente();
        model.setUsuarioCriacaoId("outro-usuario");
        when(codigoCompletoRepository.findById(model.getId())).thenReturn(Optional.of(model));

        assertThrows(BusinessException.class, () -> service.cancelarPedido(model.getId()));
        assertThrows(BusinessException.class, () -> service.atualizarPedido(model.getId(), "nova", null));
    }

    @Test
    void cancelarPedidoLiberaAIdempotencyKeyParaNovoEnvio() {
        CodigoCompleto model = pendente();
        model.setIdempotencyKey("gov-abc");
        model.setIdempotencyHash("hash");
        when(codigoCompletoRepository.findById(model.getId())).thenReturn(Optional.of(model));

        service.cancelarPedido(model.getId());

        assertNull(model.getIdempotencyKey());
        assertNull(model.getIdempotencyHash());
    }

    @Test
    void editarDescricaoReavaliaASimilaridade() {
        CodigoCompleto model = pendente();
        model.setDecisaoSimilaridade("PERMITIR");
        when(codigoCompletoRepository.findById(model.getId())).thenReturn(Optional.of(model));
        when(previewService.resolver(any(), eq(false))).thenReturn(new PreviewService.ComponentesResolvidos(
                new GrupoServico(), false, new CodigoFornecedor(), false,
                new CodigoServico(), false, new UnidadeMedida(), false));
        when(previewService.montarSnapshot(any())).thenReturn("snap novo");
        when(similaridadeService.consultar(any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(new com.gerdau.desafio.dto.response.SimilaridadeResponse(
                        "x", "x", 1, false, "DUPLICIDADE", "REVISAR", "ok", java.util.Map.of(), 1, List.of()));

        service.atualizarPedido(model.getId(), "Descricao que virou duplicidade", null);

        verify(catalogoService).atualizarDescricaoServico("CS-000010", "Descricao que virou duplicidade");
        verify(similaridadeService).consultar(eq("Descricao que virou duplicidade"),
                org.mockito.ArgumentMatchers.anyInt(), eq("CS"));
        assertEquals("REVISAR", model.getDecisaoSimilaridade(), "a decisão precisa ser refeita com a nova descrição");
    }
}
