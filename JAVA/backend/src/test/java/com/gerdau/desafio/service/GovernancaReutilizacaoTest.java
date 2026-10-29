package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.GovernancaReutilizarRequest;
import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.EventoAuditoria;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.RegistroSimilar;
import com.gerdau.desafio.model.StatusComponente;
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GovernancaReutilizacaoTest {

    private CodigoCompletoRepository codigoCompletoRepository;
    private CatalogoService catalogoService;
    private PreviewService previewService;
    private AuditoriaService auditoriaService;
    private GovernancaService service;

    @BeforeEach
    void configurar() {
        codigoCompletoRepository = Mockito.mock(CodigoCompletoRepository.class);
        catalogoService = Mockito.mock(CatalogoService.class);
        previewService = Mockito.mock(PreviewService.class);
        auditoriaService = Mockito.mock(AuditoriaService.class);
        service = new GovernancaService(Mockito.mock(MatrizDeParaRepository.class), codigoCompletoRepository,
                Mockito.mock(ExcecaoRepository.class), Mockito.mock(EventoAuditoriaRepository.class),
                auditoriaService, catalogoService, previewService, Mockito.mock(MongoTemplate.class));

        when(previewService.resolver(any(PreviewRequest.class), anyBoolean())).thenAnswer(i -> {
            PreviewRequest req = i.getArgument(0);
            GrupoServico gs = new GrupoServico();
            gs.setId(req.gsId());
            gs.setNome("GRUPO " + req.gsId());
            CodigoFornecedor cf = new CodigoFornecedor();
            cf.setId(req.cfId());
            cf.setNome("FORNECEDOR " + req.cfId());
            CodigoServico cs = new CodigoServico();
            cs.setId(req.csId());
            cs.setDescricao("SERVICO " + req.csId());
            UnidadeMedida um = new UnidadeMedida();
            um.setId(req.umId());
            um.setSigla(req.umId());
            um.setNome("UNIDADE");
            return new PreviewService.ComponentesResolvidos(gs, false, cf, false, cs, false, um, false);
        });
        when(previewService.montarSnapshot(any())).thenReturn("snapshot atualizado");
    }

    private CodigoCompleto pendenteComMatchesIgnorados() {
        CodigoCompleto model = new CodigoCompleto();
        model.setId("CIVIL_1_CS-000007_UN");
        model.setIdCompleto("CIVIL_1_CS-000007_UN");
        model.setGsId("CIVIL");
        model.setCfId("1");
        model.setCsId("CS-000007");
        model.setUmId("UN");
        model.setStatus(CodigoCompleto.STATUS_PENDENTE);
        model.setDecisaoSimilaridade("REVISAR");
        model.setSimilaresRegistrados(List.of(
                new RegistroSimilar(1, "CS-000001", null, "SV EQUIPE TELHADO", 0.93,
                        "muito parecido", false, null, "CS"),
                new RegistroSimilar(2, "GS-MECANICA", null, "MECANICA", 0.80,
                        "grupo parecido", false, null, "GS")));
        return model;
    }

    @Test
    void governancaEscolheOMatchIgnoradoEOPedidoSegue() {
        CodigoCompleto model = pendenteComMatchesIgnorados();
        when(codigoCompletoRepository.findById("CIVIL_1_CS-000007_UN")).thenReturn(Optional.of(model));
        when(codigoCompletoRepository.save(any(CodigoCompleto.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.reutilizarNovo("CIVIL_1_CS-000007_UN",
                new GovernancaReutilizarRequest("CS-000001", null, "ana", "é o mesmo serviço do pedido"));

        assertEquals(CodigoCompleto.STATUS_ATIVO, resposta.status());
        assertEquals("CS-000001", model.getCsId());
        assertTrue(model.getIdCompleto().contains("CS-000001"));
        assertTrue(model.getObservacaoAprovacao().contains("CS-000001"));
        verify(catalogoService).desativarPendentesNaoUsados(model);
        verify(catalogoService).definirStatusComponentes(model, StatusComponente.ATIVO);
        verify(auditoriaService).registrar(eq(EventoAuditoria.ACAO_REUTILIZACAO), eq("CODIGO_COMPLETO"),
                any(), any(), any(), eq(List.of("CS-000001")), any(), eq("ana"));
    }

    @Test
    void governancaEscolheTambemMatchDeOutroComponente() {
        CodigoCompleto model = pendenteComMatchesIgnorados();
        when(codigoCompletoRepository.findById("CIVIL_1_CS-000007_UN")).thenReturn(Optional.of(model));
        when(codigoCompletoRepository.save(any(CodigoCompleto.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.reutilizarNovo("CIVIL_1_CS-000007_UN",
                new GovernancaReutilizarRequest("GS-MECANICA", "GS", "ana", null));

        assertEquals(CodigoCompleto.STATUS_ATIVO, resposta.status());
        assertEquals("GS-MECANICA", model.getGsId());
        assertEquals("CS-000007", model.getCsId());
    }

    @Test
    void soReutilizaQuemEstaEntreOsMatchesApresentados() {
        CodigoCompleto model = pendenteComMatchesIgnorados();
        when(codigoCompletoRepository.findById("CIVIL_1_CS-000007_UN")).thenReturn(Optional.of(model));

        assertThrows(BusinessException.class, () -> service.reutilizarNovo("CIVIL_1_CS-000007_UN",
                new GovernancaReutilizarRequest("CS-QUALQUER", null, "ana", null)));

        verify(codigoCompletoRepository, never()).save(any());
        assertEquals("CS-000007", model.getCsId());
    }

    @Test
    void pedidoJaDecididoNaoPodeMaisReutilizar() {
        CodigoCompleto model = pendenteComMatchesIgnorados();
        model.setStatus(CodigoCompleto.STATUS_ATIVO);
        when(codigoCompletoRepository.findById("CIVIL_1_CS-000007_UN")).thenReturn(Optional.of(model));

        assertThrows(BusinessException.class, () -> service.reutilizarNovo("CIVIL_1_CS-000007_UN",
                new GovernancaReutilizarRequest("CS-000001", null, "ana", null)));
        verify(codigoCompletoRepository, never()).save(any());
    }
}
