package com.gerdau.desafio.service;

import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GovernancaComponentesTest {

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
    }

    private static GrupoServico grupo(String id, String nome, String status) {
        GrupoServico g = new GrupoServico();
        g.setId(id);
        g.setNome(nome);
        g.setStatus(status);
        return g;
    }

    private static CodigoServico servico(String id, String descricao, String status) {
        CodigoServico cs = new CodigoServico();
        cs.setId(id);
        cs.setDescricao(descricao);
        cs.setStatus(status);
        return cs;
    }

    @Test
    void listasDeUsoOcultamItensAguardandoAprovacao() {
        when(grupoRepo.findAll()).thenReturn(List.of(
                grupo("civil", "CIVIL", StatusComponente.ATIVO),
                grupo("governanca", "Governanca", StatusComponente.PENDENTE_VALIDACAO),
                grupo("rejeitado", "REJEITADO", StatusComponente.REJEITADO)));

        List<?> uso = service.grupos("", 10, false);
        assertEquals(1, uso.size());

        List<?> todos = service.grupos("", 10, true);
        assertEquals(3, todos.size());
    }

    @Test
    void documentoAntigoSemCampoStatusValeComoAtivo() {
        when(grupoRepo.findAll()).thenReturn(List.of(grupo("antigo", "ANTIGO", null)));
        assertEquals(1, service.grupos("", 10, false).size());
    }

    @Test
    void aprovacaoAtivaApenasOsComponentesQueEstavamPendentes() {
        GrupoServico pendente = grupo("novo", "NOVO", StatusComponente.PENDENTE_VALIDACAO);
        GrupoServico ativo = grupo("civil", "CIVIL", StatusComponente.ATIVO);
        CodigoServico csPendente = servico("CS-000001", "Servico novo", StatusComponente.PENDENTE_VALIDACAO);
        when(grupoRepo.findById("novo")).thenReturn(Optional.of(pendente));
        when(grupoRepo.findById("civil")).thenReturn(Optional.of(ativo));
        when(fornecedorRepo.findById(any())).thenReturn(Optional.empty());
        when(servicoRepo.findById("CS-000001")).thenReturn(Optional.of(csPendente));
        when(unidadeRepo.findById(any())).thenReturn(Optional.empty());

        CodigoCompleto codigo = new CodigoCompleto();
        codigo.setGsId("novo");
        codigo.setCfId("200982674");
        codigo.setCsId("CS-000001");
        codigo.setUmId("UN");

        service.definirStatusComponentes(codigo, StatusComponente.ATIVO);

        assertEquals(StatusComponente.ATIVO, pendente.getStatus());
        assertEquals(StatusComponente.ATIVO, csPendente.getStatus());
        verify(grupoRepo).save(pendente);
        verify(servicoRepo).save(csPendente);

        CodigoCompleto codigoReutilizado = new CodigoCompleto();
        codigoReutilizado.setGsId("civil");
        codigoReutilizado.setCfId("200982674");
        codigoReutilizado.setCsId("CS-000001");
        codigoReutilizado.setUmId("UN");
        service.definirStatusComponentes(codigoReutilizado, StatusComponente.ATIVO);
        verify(grupoRepo, never()).save(ativo);
    }

    @Test
    void rejeicaoMantemOComponenteForaDeCirculacao() {
        GrupoServico pendente = grupo("novo", "NOVO", StatusComponente.PENDENTE_VALIDACAO);
        when(grupoRepo.findById("novo")).thenReturn(Optional.of(pendente));
        when(fornecedorRepo.findById(any())).thenReturn(Optional.empty());
        when(servicoRepo.findById(any())).thenReturn(Optional.empty());
        when(unidadeRepo.findById(any())).thenReturn(Optional.empty());

        CodigoCompleto codigo = new CodigoCompleto();
        codigo.setGsId("novo");
        codigo.setCfId("200982674");
        codigo.setCsId("CS-000001");
        codigo.setUmId("UN");

        service.definirStatusComponentes(codigo, StatusComponente.REJEITADO);

        assertEquals(StatusComponente.REJEITADO, pendente.getStatus());
        verify(grupoRepo).save(pendente);
    }

    @Test
    void cadastroDiretoEntraAtivoENasListasDeUso() {
        when(grupoRepo.save(any(GrupoServico.class))).thenAnswer(i -> i.getArgument(0));
        var resposta = service.criarGrupo("Novo Grupo");
        assertEquals(StatusComponente.ATIVO, resposta.status());
    }
}
