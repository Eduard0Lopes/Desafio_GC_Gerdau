package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.response.CodigoServicoResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.StatusComponente;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogoUnicidadeTest {

    private CodigoServicoRepository servicoRepo;
    private CatalogoService service;

    @BeforeEach
    void configurar() {
        servicoRepo = Mockito.mock(CodigoServicoRepository.class);
        service = new CatalogoService(Mockito.mock(GrupoServicoRepository.class),
                Mockito.mock(CodigoFornecedorRepository.class), servicoRepo,
                Mockito.mock(UnidadeMedidaRepository.class),
                Mockito.mock(CodigoCompletoRepository.class),
                Mockito.mock(SimilaridadeService.class),
                Mockito.mock(AuditoriaService.class),
                Mockito.mock(SequenceService.class));
    }

    @Test
    void naoPermiteDoisCodigosDeServicoComOMesmoNome() {
        CodigoServico existente = new CodigoServico();
        existente.setId("CS-428555");
        existente.setDescricao("SV OFICIAL MENSAL");
        when(servicoRepo.findByDescricaoNormalizada("sv oficial mensal")).thenReturn(List.of(existente));

        assertThrows(ConflictException.class, () -> service.criarServico("SV OFICIAL MENSAL"));
        verify(servicoRepo, never()).save(any());
    }

    @Test
    void nomesDiferentesContinuamSendoCriados() {
        when(servicoRepo.findByDescricaoNormalizada(any())).thenReturn(List.of());
        when(servicoRepo.save(any(CodigoServico.class))).thenAnswer(i -> i.getArgument(0));

        CodigoServicoResponse resposta = service.criarServico("Servico inedito");

        assertEquals("Servico inedito", resposta.descricao());
        verify(servicoRepo).save(any());
    }

    @Test
    void marcarCasoIsoladoRegistraMotivoNoServico() {
        CodigoServico cs = new CodigoServico();
        cs.setId("CS-000007");
        cs.setDescricao("SV OFICIAL MENSAL");
        when(servicoRepo.findById("CS-000007")).thenReturn(Optional.of(cs));

        service.marcarCasoIsolado("CS-000007", "Duplicidade aceita pelo comite.");

        assertTrue(cs.getCasoIsolado());
        assertEquals("Duplicidade aceita pelo comite.", cs.getMotivoCasoIsolado());
        verify(servicoRepo).save(cs);
    }

    @Test
    void marcarCasoIsoladoSemServicoNaoQuebra() {
        when(servicoRepo.findById("CS-inexistente")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.marcarCasoIsolado("CS-inexistente", "motivo"));
        verify(servicoRepo, never()).save(any());
    }

    @Test
    void marcarCasosIsoladosExigeMotivoEMarcaTodos() {
        CodigoServico a = new CodigoServico();
        a.setId("CS-1");
        CodigoServico b = new CodigoServico();
        b.setId("CS-2");
        when(servicoRepo.findById("CS-1")).thenReturn(Optional.of(a));
        when(servicoRepo.findById("CS-2")).thenReturn(Optional.of(b));

        assertThrows(BusinessException.class,
                () -> service.marcarCasosIsolados(java.util.List.of("CS-1", "CS-2"), "  "));

        var resposta = service.marcarCasosIsolados(java.util.List.of("CS-1", "CS-2"), "Duplicidade tratada na varredura.");
        assertEquals(2, resposta.get("marcados"));
        assertEquals("Duplicidade tratada na varredura.", a.getMotivoCasoIsolado());
        assertTrue(b.getCasoIsolado());
    }

    @Test
    void marcarComMotivoPorItemUsaOTextoDeCadaUm() {
        CodigoServico a = new CodigoServico();
        a.setId("CS-1");
        CodigoServico b = new CodigoServico();
        b.setId("CS-2");
        when(servicoRepo.findById("CS-1")).thenReturn(Optional.of(a));
        when(servicoRepo.findById("CS-2")).thenReturn(Optional.of(b));

        service.marcarCasosIsolados(java.util.List.of("CS-1", "CS-2"), "motivo geral",
                java.util.Map.of("CS-2", "motivo do CS-2"));

        assertEquals("motivo geral", a.getMotivoCasoIsolado());
        assertEquals("motivo do CS-2", b.getMotivoCasoIsolado());
    }

    @Test
    void desativarServicosTiraDaCirculacao() {
        CodigoServico a = new CodigoServico();
        a.setId("CS-1");
        a.setStatus(StatusComponente.ATIVO);
        when(servicoRepo.findById("CS-1")).thenReturn(Optional.of(a));

        assertEquals(1, service.desativarServicos(java.util.List.of("CS-1", "CS-fantasma")));
        assertEquals(StatusComponente.DESATIVADO, a.getStatus());
    }
}
