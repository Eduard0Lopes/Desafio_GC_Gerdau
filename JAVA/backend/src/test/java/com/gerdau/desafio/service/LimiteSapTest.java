package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.similarity.RankingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LimiteSapTest {

    private PreviewService previewService;

    @BeforeEach
    void configurar() {
        GrupoServicoRepository grupoRepo = Mockito.mock(GrupoServicoRepository.class);
        CodigoFornecedorRepository fornecedorRepo = Mockito.mock(CodigoFornecedorRepository.class);
        CodigoServicoRepository servicoRepo = Mockito.mock(CodigoServicoRepository.class);
        UnidadeMedidaRepository unidadeRepo = Mockito.mock(UnidadeMedidaRepository.class);
        CodigoCompletoRepository completoRepo = Mockito.mock(CodigoCompletoRepository.class);
        SimilaridadeService similaridade = Mockito.mock(SimilaridadeService.class);

        GrupoServico grupo = new GrupoServico();
        grupo.setId("CIVIL");
        grupo.setNome("Civil");
        when(grupoRepo.findById("CIVIL")).thenReturn(Optional.of(grupo));
        when(fornecedorRepo.findById(any())).thenReturn(Optional.of(new com.gerdau.desafio.model.CodigoFornecedor()));
        when(unidadeRepo.findById(any())).thenReturn(Optional.of(new com.gerdau.desafio.model.UnidadeMedida()));
        when(completoRepo.existsByIdCompleto(any())).thenReturn(false);

        previewService = new PreviewService(grupoRepo, fornecedorRepo, servicoRepo, unidadeRepo,
                completoRepo, similaridade, new RankingPolicy(0.88, 0.70));
    }

    private PreviewRequest requisicao(String descricao) {
        return new PreviewRequest("CIVIL", null, "200982674", null, null,
                null, descricao, "SV", null, null);
    }

    @Test
    void descricaoAbaixoDe40EhAceita() {
        String descricao = "x".repeat(39);
        PreviewService.ComponentesResolvidos r = previewService.resolver(requisicao(descricao), false);
        assertEquals(39, r.codigoServico().getQuantidadeCaracteres());
    }

    @Test
    void descricaoComExatamente40EhAceita() {
        String descricao = "x".repeat(40);
        PreviewService.ComponentesResolvidos r = previewService.resolver(requisicao(descricao), false);
        assertEquals(40, r.codigoServico().getQuantidadeCaracteres());
    }

    @Test
    void descricaoAcimaDe40EhRejeitada() {
        String descricao = "x".repeat(41);
        BusinessException erro = assertThrows(BusinessException.class,
                () -> previewService.resolver(requisicao(descricao), false));
        org.junit.jupiter.api.Assertions.assertTrue(erro.getMessage().contains("40"));
    }
}
