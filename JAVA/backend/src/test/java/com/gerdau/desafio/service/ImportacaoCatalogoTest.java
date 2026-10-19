package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.ImportacaoCatalogoRequest;
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

class ImportacaoCatalogoTest {

    private GrupoServicoRepository grupoRepo;
    private CodigoFornecedorRepository fornecedorRepo;
    private CodigoServicoRepository servicoRepo;
    private UnidadeMedidaRepository unidadeRepo;
    private CatalogoService service;

    @BeforeEach
    void configurar() {
        grupoRepo = Mockito.mock(GrupoServicoRepository.class);
        fornecedorRepo = Mockito.mock(CodigoFornecedorRepository.class);
        servicoRepo = Mockito.mock(CodigoServicoRepository.class);
        unidadeRepo = Mockito.mock(UnidadeMedidaRepository.class);
        service = new CatalogoService(grupoRepo, fornecedorRepo, servicoRepo, unidadeRepo,
                Mockito.mock(CodigoCompletoRepository.class),
                Mockito.mock(SimilaridadeService.class),
                Mockito.mock(AuditoriaService.class),
                Mockito.mock(SequenceService.class));
        when(grupoRepo.save(any(GrupoServico.class))).thenAnswer(i -> i.getArgument(0));
        when(fornecedorRepo.save(any(CodigoFornecedor.class))).thenAnswer(i -> i.getArgument(0));
        when(servicoRepo.save(any(CodigoServico.class))).thenAnswer(i -> i.getArgument(0));
        when(unidadeRepo.save(any(UnidadeMedida.class))).thenAnswer(i -> i.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> importar(String tipo, ImportacaoCatalogoRequest.ItemImportacao... itens) {
        return service.importarEmMassa(tipo, List.of(itens));
    }

    @Test
    void importacaoSoAdicionaNaoApagaNada() {
        when(grupoRepo.findById(any())).thenReturn(Optional.empty());
        when(grupoRepo.findByNomeNormalizado(any())).thenReturn(List.of());

        Map<String, Object> relatorio = importar("GS",
                new ImportacaoCatalogoRequest.ItemImportacao(null, "Civil", null, null, null),
                new ImportacaoCatalogoRequest.ItemImportacao(null, "Eletrica", null, null, null));

        assertEquals(2, relatorio.get("total"));
        assertEquals(2, relatorio.get("adicionados"));
        verify(grupoRepo, times(2)).save(any(GrupoServico.class));
        verify(grupoRepo, never()).delete(any(GrupoServico.class));
    }

    @Test
    void linhaQueJaExisteEIgnoradaEItemAtualEMantido() {
        GrupoServico existente = new GrupoServico();
        existente.setId("civil");
        existente.setNome("Civil");
        when(grupoRepo.findById(any())).thenReturn(Optional.of(existente));

        Map<String, Object> relatorio = importar("GS",
                new ImportacaoCatalogoRequest.ItemImportacao(null, "Civil", null, null, null));

        assertEquals(0, relatorio.get("adicionados"));
        List<Map<String, Object>> ignorados = (List<Map<String, Object>>) relatorio.get("ignorados");
        assertEquals(1, ignorados.size());
        assertTrue(ignorados.get(0).get("motivo").toString().contains("mantido"));
        verify(grupoRepo, never()).save(any(GrupoServico.class));
        verify(grupoRepo, never()).delete(any(GrupoServico.class));
    }

    @Test
    void linhaInvalidaNaoDerrubaAsDemais() {
        when(servicoRepo.findByDescricaoNormalizada(any())).thenReturn(List.of());

        Map<String, Object> relatorio = importar("CS",
                new ImportacaoCatalogoRequest.ItemImportacao(null, null, "Troca de valvula", null, null),
                new ImportacaoCatalogoRequest.ItemImportacao(null, null, "x".repeat(41), null, null),
                new ImportacaoCatalogoRequest.ItemImportacao(null, null, "Lubrificacao", null, null));

        assertEquals(3, relatorio.get("total"));
        assertEquals(2, relatorio.get("adicionados"));
        List<Map<String, Object>> ignorados = (List<Map<String, Object>>) relatorio.get("ignorados");
        assertEquals(1, ignorados.size());
        assertEquals(2, ignorados.get(0).get("linha"));
        verify(servicoRepo, times(2)).save(any(CodigoServico.class));
    }

    @Test
    void importacaoDeUnidadeUsaSiglaComoIdentidade() {
        when(unidadeRepo.existsById("HR")).thenReturn(true);

        Map<String, Object> relatorio = importar("UM",
                new ImportacaoCatalogoRequest.ItemImportacao(null, "Hora", null, "HR", null));

        assertEquals(0, relatorio.get("adicionados"));
        verify(unidadeRepo, never()).save(any(UnidadeMedida.class));
    }
}
