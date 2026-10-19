package com.gerdau.desafio.service;

import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DesativacaoComponentesTest {

    private GrupoServicoRepository grupoRepo;
    private CatalogoService service;

    private static GrupoServico grupo(String id, String nome, String status) {
        GrupoServico g = new GrupoServico();
        g.setId(id);
        g.setNome(nome);
        g.setStatus(status);
        return g;
    }

    @BeforeEach
    void configurar() {
        grupoRepo = Mockito.mock(GrupoServicoRepository.class);
        service = new CatalogoService(grupoRepo,
                Mockito.mock(CodigoFornecedorRepository.class),
                Mockito.mock(CodigoServicoRepository.class),
                Mockito.mock(UnidadeMedidaRepository.class),
                Mockito.mock(CodigoCompletoRepository.class),
                Mockito.mock(SimilaridadeService.class),
                Mockito.mock(AuditoriaService.class),
                Mockito.mock(SequenceService.class));
        when(grupoRepo.save(any(GrupoServico.class))).thenAnswer(i -> i.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> situacao(String situacao, GrupoServico alvo) {
        when(grupoRepo.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        return service.definirSituacaoEmMassa("GS", List.of(alvo.getId()), situacao);
    }

    @Test
    void desativarTiraDeCirculacaoSemApagarNada() {
        GrupoServico ativo = grupo("civil", "CIVIL", StatusComponente.ATIVO);
        List<Map<String, Object>> relatorio = situacao("DESATIVADO", ativo);

        assertEquals(StatusComponente.DESATIVADO, ativo.getStatus());
        assertEquals(Boolean.FALSE, ativo.getActive());
        assertTrue((Boolean) relatorio.get(0).get("alterado"));
        verify(grupoRepo).save(ativo);
        verify(grupoRepo, never()).delete(any(GrupoServico.class));

        when(grupoRepo.findAll()).thenReturn(List.of(ativo));
        assertEquals(0, service.grupos("", 10, false).size());
        assertEquals(1, service.grupos("", 10, true).size());
    }

    @Test
    void reativarDevolveOItemAsListasDeUso() {
        GrupoServico desativado = grupo("civil", "CIVIL", StatusComponente.DESATIVADO);
        List<Map<String, Object>> relatorio = situacao("ATIVO", desativado);

        assertEquals(StatusComponente.ATIVO, desativado.getStatus());
        assertEquals(Boolean.TRUE, desativado.getActive());
        assertTrue((Boolean) relatorio.get(0).get("alterado"));
    }

    @Test
    void itemEmAprovacaoNaoESerDesativadoPorAqui() {
        GrupoServico pendente = grupo("novo", "NOVO", StatusComponente.PENDENTE_VALIDACAO);
        List<Map<String, Object>> relatorio = situacao("DESATIVADO", pendente);

        assertEquals(StatusComponente.PENDENTE_VALIDACAO, pendente.getStatus());
        assertFalse((Boolean) relatorio.get(0).get("alterado"));
        assertTrue(relatorio.get(0).get("motivo").toString().contains("Governan"));
        verify(grupoRepo, never()).save(any(GrupoServico.class));
    }

    @Test
    void rejeitadoPelaGovernancaNaoReativaPorAqui() {
        GrupoServico rejeitado = grupo("x", "X", StatusComponente.REJEITADO);
        List<Map<String, Object>> relatorio = situacao("ATIVO", rejeitado);

        assertEquals(StatusComponente.REJEITADO, rejeitado.getStatus());
        assertFalse((Boolean) relatorio.get(0).get("alterado"));
    }
}
