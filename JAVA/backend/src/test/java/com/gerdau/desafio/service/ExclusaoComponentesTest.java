package com.gerdau.desafio.service;

import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.model.EventoAuditoria;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExclusaoComponentesTest {

    private GrupoServicoRepository grupoRepo;
    private CodigoCompletoRepository codigoCompletoRepository;
    private SimilaridadeService similaridadeService;
    private AuditoriaService auditoriaService;
    private CatalogoService service;

    private static GrupoServico grupo() {
        GrupoServico g = new GrupoServico();
        g.setId("TESTE");
        g.setNome("Teste");
        return g;
    }

    @BeforeEach
    void configurar() {
        grupoRepo = Mockito.mock(GrupoServicoRepository.class);
        codigoCompletoRepository = Mockito.mock(CodigoCompletoRepository.class);
        similaridadeService = Mockito.mock(SimilaridadeService.class);
        auditoriaService = Mockito.mock(AuditoriaService.class);
        service = new CatalogoService(grupoRepo,
                Mockito.mock(CodigoFornecedorRepository.class),
                Mockito.mock(CodigoServicoRepository.class),
                Mockito.mock(UnidadeMedidaRepository.class),
                codigoCompletoRepository,
                similaridadeService,
                auditoriaService,
                Mockito.mock(SequenceService.class));
    }

    @Test
    void exclusaoBloqueadaQuandoHaVinculos() {
        when(grupoRepo.findById("TESTE")).thenReturn(Optional.of(grupo()));
        when(codigoCompletoRepository.countByGsId("TESTE")).thenReturn(3L);

        ConflictException erro = assertThrows(ConflictException.class, () -> service.excluirGrupo("TESTE"));

        assertTrue(erro.getMessage().contains("vinculado(a) a 3 código(s) completo(s)"));
        verify(grupoRepo, never()).delete(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void exclusaoSemVinculosRemoveAuditaEInvalidaCache() {
        when(grupoRepo.findById("TESTE")).thenReturn(Optional.of(grupo()));
        when(codigoCompletoRepository.countByGsId("TESTE")).thenReturn(0L);

        service.excluirGrupo("TESTE");

        verify(grupoRepo).delete(any(GrupoServico.class));
        verify(similaridadeService).invalidarCache();
        ArgumentCaptor<String> acao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(acao.capture(), eq("GRUPO_SERVICO"),
                eq("TESTE"), any(), any(), any(), any(), any());
        assertEquals(EventoAuditoria.ACAO_EXCLUSAO_COMPONENTE, acao.getValue());
    }

    @Test
    void exclusaoDeItemInexistenteGeraNotFound() {
        when(grupoRepo.findById("NAO_EXISTE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.excluirGrupo("NAO_EXISTE"));
        verify(grupoRepo, never()).delete(any());
    }
}
