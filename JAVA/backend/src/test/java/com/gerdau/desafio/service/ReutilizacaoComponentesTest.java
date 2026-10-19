package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.dto.response.PreviewResponse;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReutilizacaoComponentesTest {

    private GrupoServicoRepository grupoRepo;
    private CodigoFornecedorRepository fornecedorRepo;
    private CodigoServicoRepository servicoRepo;
    private UnidadeMedidaRepository unidadeRepo;
    private PreviewService previewService;

    private static GrupoServico grupoGovernanca() {
        GrupoServico g = new GrupoServico();
        g.setId("GOVERNANCA");
        g.setNome("Governança");
        g.setNomeNormalizado("governanca");
        return g;
    }

    private static UnidadeMedida unidadeUn() {
        UnidadeMedida u = new UnidadeMedida();
        u.setId("UN");
        u.setSigla("UN");
        u.setNome("Unidade");
        return u;
    }

    @BeforeEach
    void configurar() {
        grupoRepo = Mockito.mock(GrupoServicoRepository.class);
        fornecedorRepo = Mockito.mock(CodigoFornecedorRepository.class);
        servicoRepo = Mockito.mock(CodigoServicoRepository.class);
        unidadeRepo = Mockito.mock(UnidadeMedidaRepository.class);
        previewService = new PreviewService(grupoRepo, fornecedorRepo, servicoRepo, unidadeRepo,
                Mockito.mock(CodigoCompletoRepository.class),
                Mockito.mock(SimilaridadeService.class),
                Mockito.mock(com.gerdau.desafio.similarity.RankingPolicy.class));
    }

    @Test
    void dadosNovosQueJaExistemReutilizamOsItensExistentes() {
        GrupoServico grupo = grupoGovernanca();
        UnidadeMedida unidade = unidadeUn();
        CodigoFornecedor fornecedor = new CodigoFornecedor();
        fornecedor.setId("1234567");
        fornecedor.setNome("Governança");
        fornecedor.setNomeNormalizado("governanca");

        when(grupoRepo.findById("GOVERNANCA")).thenReturn(Optional.empty());
        when(grupoRepo.findByNomeNormalizado("governanca")).thenReturn(List.of(grupo));
        when(fornecedorRepo.findById(any())).thenReturn(Optional.empty());
        when(fornecedorRepo.findByNomeNormalizado("governanca")).thenReturn(List.of(fornecedor));
        when(unidadeRepo.findById("UN")).thenReturn(Optional.of(unidade));

        PreviewRequest req = new PreviewRequest(null, "Governança", null, "Governança", null,
                null, "Troca de válvula", null, "UN", null);
        PreviewService.ComponentesResolvidos r = previewService.resolver(req, true);

        assertFalse(r.grupoNovo(), "grupo já existente deve ser reutilizado");
        assertSame(grupo, r.grupoServico());
        assertFalse(r.fornecedorNovo(), "fornecedor já existente deve ser reutilizado");
        assertSame(fornecedor, r.codigoFornecedor());
        assertFalse(r.unidadeNova(), "unidade já existente deve ser reutilizada");
        assertSame(unidade, r.unidadeMedida());
        assertTrue(r.codigoNovo(), "o serviço novo continua novo (identidade é o código)");
    }

    @Test
    void descricaoIgualReutilizaOCodigoDeServicoExistente() {
        CodigoServico existente = new CodigoServico();
        existente.setId("CS-428555");
        existente.setDescricao("SV OFICIAL MENSAL");

        when(grupoRepo.findById(anyString())).thenReturn(Optional.empty());
        when(grupoRepo.findByNomeNormalizado(anyString())).thenReturn(List.of());
        when(fornecedorRepo.findById(any())).thenReturn(Optional.empty());
        when(fornecedorRepo.findByNomeNormalizado(anyString())).thenReturn(List.of());
        when(unidadeRepo.findById(anyString())).thenReturn(Optional.empty());
        when(servicoRepo.findByDescricaoNormalizada("sv oficial mensal")).thenReturn(List.of(existente));

        PreviewRequest req = new PreviewRequest(null, "Novo Grupo", null, "Novo Fornecedor", null,
                null, "SV OFICIAL MENSAL", null, "UN", null);
        PreviewService.ComponentesResolvidos r = previewService.resolver(req, true);

        assertFalse(r.codigoNovo(), "nao pode existir outro CS com o mesmo nome");
        assertSame(existente, r.codigoServico());
    }

    @Test
    void dadosNovosDeFatoContinuamSendoCriados() {
        when(grupoRepo.findById(anyString())).thenReturn(Optional.empty());
        when(grupoRepo.findByNomeNormalizado(anyString())).thenReturn(List.of());
        when(fornecedorRepo.findById(any())).thenReturn(Optional.empty());
        when(fornecedorRepo.findByNomeNormalizado(anyString())).thenReturn(List.of());
        when(unidadeRepo.findById(anyString())).thenReturn(Optional.empty());

        PreviewRequest req = new PreviewRequest(null, "Novo Grupo", null, "Novo Fornecedor", null,
                null, "Serviço qualquer", null, "UN", null);
        PreviewService.ComponentesResolvidos r = previewService.resolver(req, true);

        assertTrue(r.grupoNovo());
        assertTrue(r.fornecedorNovo());
        assertTrue(r.unidadeNova());
        assertTrue(r.codigoNovo());
    }

    @Test
    void conflitoEmQualquerComponenteNaoDeixaCadastroParcial() {
        CatalogoService catalogoService = Mockito.mock(CatalogoService.class);
        CodigoCompletoRepository codigoCompletoRepository = Mockito.mock(CodigoCompletoRepository.class);
        AuditoriaService auditoriaService = Mockito.mock(AuditoriaService.class);

        GrupoServico grupo = grupoGovernanca();
        CodigoFornecedor fornecedor = new CodigoFornecedor();
        fornecedor.setId("1234567");
        fornecedor.setNome("Fornecedor");
        CodigoServico servico = new CodigoServico();
        servico.setId("(novo)");
        servico.setDescricao("Troca de válvula");
        UnidadeMedida unidade = unidadeUn();

        PreviewService preview = Mockito.mock(PreviewService.class);
        when(preview.montarPreview(any())).thenReturn(new PreviewResponse("X",
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("GOVERNANCA", "Governança", true),
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("1234567", "Fornecedor", false),
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("(novo)", "Troca de válvula", true),
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("UN", "UN - Unidade", true),
                "snap", 15, false, false, null, "ORIGINAL", "PERMITIR", "ok", 0.0, List.of(), List.of()));
        when(preview.resolver(any(), anyBoolean())).thenReturn(new PreviewService.ComponentesResolvidos(
                grupo, true, fornecedor, false, servico, true, unidade, true));

        doThrow(new ConflictException("Já existe uma Unidade de Medida com a sigla UN. Reutilize o item existente."))
                .when(catalogoService).exigirUnidadeLivre("UN");

        CodigoCompletoService service = new CodigoCompletoService(codigoCompletoRepository,
                Mockito.mock(ExcecaoRepository.class), preview, catalogoService,
                Mockito.mock(SimilaridadeService.class), auditoriaService, "enforce");

        CriarCodigoCompletoRequest requisicao = new CriarCodigoCompletoRequest(
                null, "Governança", null, null, null, null, "Troca de válvula",
                null, "UN", null, null, "CRIAR", "403921");

        assertThrows(ConflictException.class, () -> service.criar(requisicao, null));

        verify(catalogoService, never()).criarGrupo(any(), any());
        verify(catalogoService, never()).criarFornecedor(any(), any(), any());
        verify(catalogoService, never()).criarServico(any(), any());
        verify(catalogoService, never()).criarUnidade(any(), any(), any());
        verify(codigoCompletoRepository, never()).save(any());
    }

    @Test
    void todosOsComponentesValidadosAntesDeQualquerGravacao() {
        CatalogoService catalogoService = Mockito.mock(CatalogoService.class);
        CodigoCompletoRepository codigoCompletoRepository = Mockito.mock(CodigoCompletoRepository.class);
        when(codigoCompletoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GrupoServico grupo = new GrupoServico();
        grupo.setId("(novo)");
        grupo.setNome("Novo Grupo");
        CodigoFornecedor fornecedor = new CodigoFornecedor();
        fornecedor.setId("7654321");
        fornecedor.setNome("Fornecedor");
        when(catalogoService.criarGrupo("Novo Grupo", StatusComponente.ATIVO)).thenReturn(
                new com.gerdau.desafio.dto.response.GrupoServicoResponse("NOVO_GRUPO", "Novo Grupo", "novo grupo", "SISTEMA", "ATIVO", null, null));
        CodigoServico servico = new CodigoServico();
        servico.setId("CS-000042");
        servico.setDescricao("Troca de válvula");
        when(catalogoService.criarServico("Troca de válvula", StatusComponente.ATIVO)).thenReturn(
                new com.gerdau.desafio.dto.response.CodigoServicoResponse("CS-000042", "Troca de válvula",
                        "troca de valvula", 15, "SISTEMA", List.of(), "ATIVO", null, null, false, null));
        UnidadeMedida unidade = unidadeUn();

        PreviewService preview = Mockito.mock(PreviewService.class);
        when(preview.montarPreview(any())).thenReturn(new PreviewResponse("X",
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("(novo)", "Novo Grupo", true),
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("7654321", "Fornecedor", false),
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("(novo)", "Troca de válvula", true),
                new com.gerdau.desafio.dto.response.ComponentePreviewResponse("UN", "UN - Unidade", false),
                "snap", 15, false, false, null, "ORIGINAL", "PERMITIR", "ok", 0.0, List.of(), List.of()));
        when(preview.resolver(any(), anyBoolean())).thenReturn(new PreviewService.ComponentesResolvidos(
                grupo, true, fornecedor, false, servico, true, unidade, false));
        when(preview.montarSnapshot(any())).thenReturn("snap");

        CodigoCompletoService service = new CodigoCompletoService(codigoCompletoRepository,
                Mockito.mock(ExcecaoRepository.class), preview, catalogoService,
                Mockito.mock(SimilaridadeService.class), Mockito.mock(AuditoriaService.class), "enforce");

        CriarCodigoCompletoRequest requisicao = new CriarCodigoCompletoRequest(
                null, "Novo Grupo", "7654321", null, null, null, "Troca de válvula",
                "UN", null, null, null, "CRIAR", "403921");
        service.criar(requisicao, null);

        var ordem = inOrder(catalogoService);
        ordem.verify(catalogoService).exigirGrupoLivre("Novo Grupo");
        ordem.verify(catalogoService).criarGrupo("Novo Grupo", StatusComponente.ATIVO);
        verify(codigoCompletoRepository).save(any());
    }
}
