package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.response.*;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.search.SearchClient;
import com.gerdau.desafio.seed.DataSeeder;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogos de componentes (seção 7): listagem/consulta para os seletores e
 * cadastro de novos itens COM a regra anti-duplicidade aplicada a todos eles.
 */
@Service
public class CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);

    private final GrupoServicoRepository grupoServicoRepository;
    private final CodigoFornecedorRepository codigoFornecedorRepository;
    private final CodigoServicoRepository codigoServicoRepository;
    private final UnidadeMedidaRepository unidadeMedidaRepository;
    private final SimilaridadeService similaridadeService;
    private final AuditoriaService auditoriaService;
    private final SequenceService sequenceService;

    public CatalogoService(GrupoServicoRepository grupoServicoRepository,
                           CodigoFornecedorRepository codigoFornecedorRepository,
                           CodigoServicoRepository codigoServicoRepository,
                           UnidadeMedidaRepository unidadeMedidaRepository,
                           SimilaridadeService similaridadeService,
                           AuditoriaService auditoriaService,
                           SequenceService sequenceService) {
        this.grupoServicoRepository = grupoServicoRepository;
        this.codigoFornecedorRepository = codigoFornecedorRepository;
        this.codigoServicoRepository = codigoServicoRepository;
        this.unidadeMedidaRepository = unidadeMedidaRepository;
        this.similaridadeService = similaridadeService;
        this.auditoriaService = auditoriaService;
        this.sequenceService = sequenceService;
    }

    // ---------- consultas (seção 10.1 — ?query= para os seletores) ----------

    public List<GrupoServicoResponse> grupos(String query, int limite) {
        List<GrupoServico> itens = (query == null || query.isBlank())
                ? grupoServicoRepository.findAll()
                : grupoServicoRepository.buscarPorTexto(regex(query));
        return itens.stream().limit(limite).map(g ->
                new GrupoServicoResponse(g.getId(), g.getNome(), g.getNomeNormalizado(), g.getOrigem())).toList();
    }

    public List<CodigoFornecedorResponse> fornecedores(String query, int limite) {
        List<CodigoFornecedor> itens = (query == null || query.isBlank())
                ? codigoFornecedorRepository.findAll()
                : codigoFornecedorRepository.buscarPorTexto(regex(query));
        return itens.stream().limit(limite).map(f ->
                new CodigoFornecedorResponse(f.getId(), f.getCodigoFornecedorSAP(), f.getNome(),
                        f.getNomeNormalizado(), f.getOrigem())).toList();
    }

    public List<CodigoServicoResponse> servicos(String query, int limite) {
        List<CodigoServico> itens = (query == null || query.isBlank())
                ? codigoServicoRepository.findAll()
                : codigoServicoRepository.buscarPorTexto(regex(query));
        return itens.stream().limit(limite).map(this::servicoResponse).toList();
    }

    public List<UnidadeMedidaResponse> unidades(String query, int limite) {
        List<UnidadeMedida> itens = (query == null || query.isBlank())
                ? unidadeMedidaRepository.findAll()
                : unidadeMedidaRepository.buscarPorTexto(regex(query));
        return itens.stream().limit(limite).map(u ->
                new UnidadeMedidaResponse(u.getId(), u.getSigla(), u.getNome(), u.getNomeNormalizado(),
                        u.getOrigem())).toList();
    }

    public CodigoServicoResponse servicoPorId(String id) {
        CodigoServico cs = codigoServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código de Serviço", id));
        return servicoResponse(cs);
    }

    // ---------- cadastro de novos itens (seção 11.7) ----------

    public GrupoServicoResponse criarGrupo(String nome) {
        GrupoServico g = new GrupoServico();
        g.setId(DataSeeder.slug(nome));
        if (grupoServicoRepository.existsById(g.getId())) {
            throw new ConflictException("Já existe um Grupo de Serviço com este nome (\"" + nome.trim()
                    + "\"). Reutilize o item existente.");
        }
        g.setNome(nome.trim());
        g.setNomeNormalizado(TextoUtil.normalizar(nome));
        g.setOrigem("SISTEMA");
        g.setCreatedAt(java.time.Instant.now());
        g.setUpdatedAt(java.time.Instant.now());
        GrupoServico salvo = grupoServicoRepository.save(g);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "GRUPO_SERVICO", salvo.getId(), salvo.getNome());
        log.info("[CATALOGO] Grupo criado: {}", salvo.getId());
        return new GrupoServicoResponse(salvo.getId(), salvo.getNome(), salvo.getNomeNormalizado(), salvo.getOrigem());
    }

    public CodigoFornecedorResponse criarFornecedor(String nome, String codigoSap) {
        CodigoFornecedor f = new CodigoFornecedor();
        if (codigoSap != null && !codigoSap.isBlank()) {
            f.setId(codigoSap.trim());
            f.setCodigoFornecedorSAP(Long.parseLong(codigoSap.trim()));
        } else {
            f.setId(String.valueOf(System.currentTimeMillis() % 10_000_000));
            f.setCodigoFornecedorSAP(Long.parseLong(f.getId()));
        }
        if (codigoFornecedorRepository.existsById(f.getId())) {
            throw new ConflictException("Já existe um fornecedor com o código " + f.getId()
                    + ". Reutilize o item existente.");
        }
        f.setNome(nome.trim());
        f.setNomeNormalizado(TextoUtil.normalizar(nome));
        f.setOrigem("SISTEMA");
        f.setCreatedAt(java.time.Instant.now());
        f.setUpdatedAt(java.time.Instant.now());
        CodigoFornecedor salvo = codigoFornecedorRepository.save(f);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "CODIGO_FORNECEDOR", salvo.getId(), salvo.getNome());
        log.info("[CATALOGO] Fornecedor criado: {}", salvo.getId());
        return new CodigoFornecedorResponse(salvo.getId(), salvo.getCodigoFornecedorSAP(), salvo.getNome(),
                salvo.getNomeNormalizado(), salvo.getOrigem());
    }

    public CodigoServicoResponse criarServico(String descricao) {
        CodigoServico cs = new CodigoServico();
        cs.setId(novoIdServico());
        cs.setDescricao(descricao.trim());
        cs.setDescricaoNormalizada(TextoUtil.normalizar(descricao));
        cs.setQuantidadeCaracteres(descricao.trim().length());
        cs.setOrigem("SISTEMA");
        cs.setLegacyIds(List.of());
        cs.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(descricao));
        cs.setCreatedAt(java.time.Instant.now());
        cs.setUpdatedAt(java.time.Instant.now());
        CodigoServico salvo = codigoServicoRepository.save(cs);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "CODIGO_SERVICO", salvo.getId(), salvo.getDescricao());
        log.info("[CATALOGO] Serviço criado: {} ({})", salvo.getId(), salvo.getDescricao());
        return servicoResponse(salvo);
    }

    public UnidadeMedidaResponse criarUnidade(String sigla, String nome) {
        UnidadeMedida u = new UnidadeMedida();
        u.setId(sigla.trim().toUpperCase());
        if (unidadeMedidaRepository.existsById(u.getId())) {
            throw new ConflictException("Já existe uma Unidade de Medida com a sigla " + u.getId()
                    + ". Reutilize o item existente.");
        }
        u.setSigla(sigla.trim().toUpperCase());
        u.setNome((nome == null || nome.isBlank()) ? sigla.trim().toUpperCase() : nome.trim());
        u.setNomeNormalizado(TextoUtil.normalizar(u.getNome()));
        u.setOrigem("SISTEMA");
        u.setCreatedAt(java.time.Instant.now());
        u.setUpdatedAt(java.time.Instant.now());
        UnidadeMedida salva = unidadeMedidaRepository.save(u);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "UNIDADE_MEDIDA", salva.getId(), salva.getNome());
        log.info("[CATALOGO] Unidade criada: {}", salva.getId());
        return new UnidadeMedidaResponse(salva.getId(), salva.getSigla(), salva.getNome(),
                salva.getNomeNormalizado(), salva.getOrigem());
    }

    /** Registro de auditoria da criação de componente (ator vem do header quando não explícito). */
    private void auditar(String acao, String entityType, String entityId, String descricao) {
        auditoriaService.registrar(acao, entityType, entityId, "PERMITIR",
                "Cadastro de componente: " + descricao, java.util.List.of(), java.util.List.of(), null);
    }

    /** Verifica duplicidade de um novo item em qualquer catálogo (mesma regra para todos). */
    public List<CandidatoSimilarResponse> verificarDuplicidade(String texto, String tipo, int limite) {
        return similaridadeService.consultar(texto, limite, tipo).candidatos();
    }

    /** Próximo id no formato oficial CS-XXXXXX (sequência atômica — seção 5). */
    private String novoIdServico() {
        return sequenceService.proximoCodigoServico();
    }

    private CodigoServicoResponse servicoResponse(CodigoServico cs) {
        return new CodigoServicoResponse(cs.getId(), cs.getDescricao(), cs.getDescricaoNormalizada(),
                cs.getQuantidadeCaracteres(), cs.getOrigem(),
                cs.getLegacyIds() == null ? List.of() : cs.getLegacyIds());
    }

    private static String regex(String termo) {
        return "(?i)" + java.util.regex.Pattern.quote(termo.trim());
    }
}
