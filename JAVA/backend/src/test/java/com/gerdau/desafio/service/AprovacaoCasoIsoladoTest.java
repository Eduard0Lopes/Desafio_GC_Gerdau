package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.GovernancaDecisaoRequest;
import com.gerdau.desafio.dto.request.RevisaoMatrizRequest;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.MatrizDePara;
import com.gerdau.desafio.model.StatusComponente;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AprovacaoCasoIsoladoTest {

    private CodigoCompletoRepository codigoCompletoRepository;
    private MatrizDeParaRepository matrizDeParaRepository;
    private CatalogoService catalogoService;
    private GovernancaService service;

    @BeforeEach
    void configurar() {
        codigoCompletoRepository = Mockito.mock(CodigoCompletoRepository.class);
        matrizDeParaRepository = Mockito.mock(MatrizDeParaRepository.class);
        catalogoService = Mockito.mock(CatalogoService.class);
        service = new GovernancaService(matrizDeParaRepository, codigoCompletoRepository,
                Mockito.mock(ExcecaoRepository.class), Mockito.mock(EventoAuditoriaRepository.class),
                Mockito.mock(AuditoriaService.class), catalogoService, Mockito.mock(PreviewService.class),
                Mockito.mock(MongoTemplate.class));
    }

    private CodigoCompleto pendente(String decisao) {
        CodigoCompleto model = new CodigoCompleto();
        model.setId("CIVIL_1_CS-000007_UN");
        model.setIdCompleto("CIVIL_1_CS-000007_UN");
        model.setCsId("CS-000007");
        model.setStatus(CodigoCompleto.STATUS_PENDENTE);
        model.setDecisaoSimilaridade(decisao);
        return model;
    }

    @Test
    void aprovarDuplicidadeSemMotivoEBloqueado() {
        when(codigoCompletoRepository.findById("CIVIL_1_CS-000007_UN"))
                .thenReturn(Optional.of(pendente("REVISAR")));

        assertThrows(BusinessException.class,
                () -> service.aprovarNovo("CIVIL_1_CS-000007_UN", new GovernancaDecisaoRequest("ana", " ")));
        verify(codigoCompletoRepository, never()).save(any());
    }

    @Test
    void aprovarDuplicidadeComMotivoMarcaCasoIsolado() {
        CodigoCompleto model = pendente("REVISAR");
        when(codigoCompletoRepository.findById("CIVIL_1_CS-000007_UN")).thenReturn(Optional.of(model));
        when(codigoCompletoRepository.save(any(CodigoCompleto.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.aprovarNovo("CIVIL_1_CS-000007_UN",
                new GovernancaDecisaoRequest("ana", "Caso isolado aprovado no comite."));

        assertEquals(CodigoCompleto.STATUS_ATIVO, resposta.status());
        verify(catalogoService).marcarCasoIsolado("CS-000007", "Caso isolado aprovado no comite.");
    }

    @Test
    void aprovarSemSimilaridadeNaoExigeMotivoNemMarca() {
        CodigoCompleto model = pendente("PERMITIR");
        when(codigoCompletoRepository.findById("CIVIL_1_CS-000007_UN")).thenReturn(Optional.of(model));
        when(codigoCompletoRepository.save(any(CodigoCompleto.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.aprovarNovo("CIVIL_1_CS-000007_UN", new GovernancaDecisaoRequest("ana", null));

        assertEquals(CodigoCompleto.STATUS_ATIVO, resposta.status());
        verify(catalogoService, never()).marcarCasoIsolado(any(), any());
    }

    @Test
    void filaDeDecisaoSoTrazPendentes() {
        when(matrizDeParaRepository.findByDecisionAndStatusRevisaoOrderByScoreDesc(
                eq("DUPLICIDADE"), eq("PENDENTE"), any())).thenReturn(List.of());

        service.listarPendentes("TODOS", "", 10, "TODOS", "DUPLICIDADE", 0);

        verify(matrizDeParaRepository).findByDecisionAndStatusRevisaoOrderByScoreDesc(
                eq("DUPLICIDADE"), eq("PENDENTE"), any());
        verify(matrizDeParaRepository, never()).findByDecisionOrderByScoreDesc(any(), any());
    }

    @Test
    void excluirDoHistoricoTiraOItemDaListaAtiva() {
        MatrizDePara doc = new MatrizDePara();
        doc.setNewId("CS-428555");
        doc.setDecision("DUPLICIDADE");
        doc.setStatusRevisao(MatrizDePara.STATUS_PENDENTE);
        when(matrizDeParaRepository.findById("mat-1")).thenReturn(Optional.of(doc));
        when(matrizDeParaRepository.save(any(MatrizDePara.class))).thenAnswer(i -> i.getArgument(0));

        service.revisarHistorico("mat-1", new RevisaoMatrizRequest("ana", "EXCLUIR", "duplicado"));

        verify(catalogoService).definirStatusServico("CS-428555", StatusComponente.DESATIVADO);
        verify(catalogoService, never()).marcarCasoIsolado(any(), any());
    }

    @Test
    void aceitarDuplicidadeDoHistoricoMarcaExcecao() {
        MatrizDePara doc = new MatrizDePara();
        doc.setNewId("CS-428555");
        doc.setDecision("DUPLICIDADE");
        doc.setStatusRevisao(MatrizDePara.STATUS_PENDENTE);
        when(matrizDeParaRepository.findById("mat-2")).thenReturn(Optional.of(doc));
        when(matrizDeParaRepository.save(any(MatrizDePara.class))).thenAnswer(i -> i.getArgument(0));

        service.revisarHistorico("mat-2", new RevisaoMatrizRequest("ana", "MANTER", "uso legitimo em outro contexto"));

        verify(catalogoService).marcarCasoIsolado("CS-428555", "uso legitimo em outro contexto");
        verify(catalogoService, never()).definirStatusServico(any(), any());
    }

    @Test
    void decisaoComAcaoInvalidaOuItemJaDecididoEBloqueada() {
        MatrizDePara doc = new MatrizDePara();
        doc.setNewId("CS-428555");
        doc.setDecision("DUPLICIDADE");
        doc.setStatusRevisao(MatrizDePara.STATUS_PENDENTE);
        when(matrizDeParaRepository.findById("mat-3")).thenReturn(Optional.of(doc));

        assertThrows(BusinessException.class,
                () -> service.revisarHistorico("mat-3", new RevisaoMatrizRequest("ana", "APAGAR", "typo")));

        doc.setStatusRevisao(MatrizDePara.STATUS_REVISADO);
        assertThrows(BusinessException.class,
                () -> service.revisarHistorico("mat-3", new RevisaoMatrizRequest("ana", "MANTER", "de novo")));
    }
}
