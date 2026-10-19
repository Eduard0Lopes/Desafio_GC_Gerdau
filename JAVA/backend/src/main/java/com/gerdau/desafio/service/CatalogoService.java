package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.ImportacaoCatalogoRequest;
import com.gerdau.desafio.dto.response.*;
import com.gerdau.desafio.exception.BusinessException;
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

@Service
public class CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);

    private final GrupoServicoRepository grupoServicoRepository;
    private final CodigoFornecedorRepository codigoFornecedorRepository;
    private final CodigoServicoRepository codigoServicoRepository;
    private final UnidadeMedidaRepository unidadeMedidaRepository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final SimilaridadeService similaridadeService;
    private final AuditoriaService auditoriaService;
    private final SequenceService sequenceService;

    public CatalogoService(GrupoServicoRepository grupoServicoRepository,
                           CodigoFornecedorRepository codigoFornecedorRepository,
                           CodigoServicoRepository codigoServicoRepository,
                           UnidadeMedidaRepository unidadeMedidaRepository,
                           CodigoCompletoRepository codigoCompletoRepository,
                           SimilaridadeService similaridadeService,
                           AuditoriaService auditoriaService,
                           SequenceService sequenceService) {
        this.grupoServicoRepository = grupoServicoRepository;
        this.codigoFornecedorRepository = codigoFornecedorRepository;
        this.codigoServicoRepository = codigoServicoRepository;
        this.unidadeMedidaRepository = unidadeMedidaRepository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.similaridadeService = similaridadeService;
        this.auditoriaService = auditoriaService;
        this.sequenceService = sequenceService;
    }

    public List<GrupoServicoResponse> grupos(String query, int limite, boolean incluirTodos) {
        List<GrupoServico> itens = (query == null || query.isBlank())
                ? grupoServicoRepository.findAll()
                : grupoServicoRepository.buscarPorTexto(regex(query));
        return apenasAtivos(itens, incluirTodos, GrupoServico::getStatus).stream().limit(limite).map(g ->
                paraGrupo(g)).toList();
    }

    public List<CodigoFornecedorResponse> fornecedores(String query, int limite, boolean incluirTodos) {
        List<CodigoFornecedor> itens = (query == null || query.isBlank())
                ? codigoFornecedorRepository.findAll()
                : codigoFornecedorRepository.buscarPorTexto(regex(query));
        return apenasAtivos(itens, incluirTodos, CodigoFornecedor::getStatus).stream().limit(limite).map(f ->
                paraFornecedor(f)).toList();

    }

    public List<CodigoServicoResponse> servicos(String query, int limite, boolean incluirTodos) {
        List<CodigoServico> itens = (query == null || query.isBlank())
                ? codigoServicoRepository.findAll()
                : codigoServicoRepository.buscarPorTexto(regex(query));
        return apenasAtivos(itens, incluirTodos, CodigoServico::getStatus).stream().limit(limite).map(this::servicoResponse).toList();
    }

    public List<UnidadeMedidaResponse> unidades(String query, int limite, boolean incluirTodos) {
        List<UnidadeMedida> itens = (query == null || query.isBlank())
                ? unidadeMedidaRepository.findAll()
                : unidadeMedidaRepository.buscarPorTexto(regex(query));
        return apenasAtivos(itens, incluirTodos, UnidadeMedida::getStatus).stream().limit(limite).map(u ->
                paraUnidade(u)).toList();

    }

    public CodigoServicoResponse servicoPorId(String id) {
        CodigoServico cs = codigoServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código de Serviço", id));
        return servicoResponse(cs);
    }

    public GrupoServicoResponse criarGrupo(String nome) {
        return criarGrupo(nome, StatusComponente.ATIVO);
    }

    public GrupoServicoResponse criarGrupo(String nome, String status) {
        GrupoServico g = new GrupoServico();
        g.setId(DataSeeder.slug(nome));
        exigirGrupoLivre(nome);
        g.setNome(nome.trim());
        g.setNomeNormalizado(TextoUtil.normalizar(nome));
        g.setOrigem("SISTEMA");
        g.setStatus(StatusComponente.normalizar(status));
        g.setCriadoPor(AuditoriaService.usuarioAtual());
        g.setCreatedAt(java.time.Instant.now());
        g.setUpdatedAt(java.time.Instant.now());
        GrupoServico salvo = grupoServicoRepository.save(g);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "GRUPO_SERVICO", salvo.getId(), salvo.getNome());
        log.info("[CATALOGO] Grupo criado: {}", salvo.getId());
        return paraGrupo(salvo);
    }

    public CodigoFornecedorResponse criarFornecedor(String nome, String codigoSap) {
        return criarFornecedor(nome, codigoSap, StatusComponente.ATIVO);
    }

    public CodigoFornecedorResponse criarFornecedor(String nome, String codigoSap, String status) {
        CodigoFornecedor f = new CodigoFornecedor();
        if (codigoSap != null && !codigoSap.isBlank()) {
            f.setId(codigoSap.trim());
            f.setCodigoFornecedorSAP(Long.parseLong(codigoSap.trim()));
        } else {
            
            long candidato = System.currentTimeMillis() % 10_000_000;
            while (codigoFornecedorRepository.existsById(String.valueOf(candidato))) {
                candidato = (candidato + 1) % 10_000_000;
            }
            f.setId(String.valueOf(candidato));
            f.setCodigoFornecedorSAP(candidato);
        }
        if (codigoFornecedorRepository.existsById(f.getId())) {
            throw new ConflictException("Já existe um fornecedor com o código " + f.getId()
                    + ". Reutilize o item existente.");
        }
        f.setNome(nome.trim());
        f.setNomeNormalizado(TextoUtil.normalizar(nome));
        f.setOrigem("SISTEMA");
        f.setStatus(StatusComponente.normalizar(status));
        f.setCriadoPor(AuditoriaService.usuarioAtual());
        f.setCreatedAt(java.time.Instant.now());
        f.setUpdatedAt(java.time.Instant.now());
        CodigoFornecedor salvo = codigoFornecedorRepository.save(f);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "CODIGO_FORNECEDOR", salvo.getId(), salvo.getNome());
        log.info("[CATALOGO] Fornecedor criado: {}", salvo.getId());
        return paraFornecedor(salvo);

    }

    public CodigoServicoResponse criarServico(String descricao) {
        return criarServico(descricao, StatusComponente.ATIVO);
    }

    public CodigoServicoResponse criarServico(String descricao, String status) {
        exigirServicoLivre(descricao);
        if (TextoUtil.excedeLimiteSap(descricao == null ? "" : descricao.trim())) {
            throw new BusinessException("A descrição do serviço não pode exceder "
                    + TextoUtil.LIMITE_SAP + " caracteres (regra SAP).");
        }
        CodigoServico cs = new CodigoServico();
        cs.setId(novoIdServico());
        cs.setDescricao(descricao.trim());
        cs.setDescricaoNormalizada(TextoUtil.normalizar(descricao));
        cs.setQuantidadeCaracteres(descricao.trim().length());
        cs.setOrigem("SISTEMA");
        cs.setStatus(StatusComponente.normalizar(status));
        cs.setLegacyIds(List.of());
        cs.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(descricao));
        cs.setCriadoPor(AuditoriaService.usuarioAtual());
        cs.setCreatedAt(java.time.Instant.now());
        cs.setUpdatedAt(java.time.Instant.now());
        CodigoServico salvo = codigoServicoRepository.save(cs);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "CODIGO_SERVICO", salvo.getId(), salvo.getDescricao());
        log.info("[CATALOGO] Serviço criado: {} ({})", salvo.getId(), salvo.getDescricao());
        return servicoResponse(salvo);
    }

    public UnidadeMedidaResponse criarUnidade(String sigla, String nome) {
        return criarUnidade(sigla, nome, StatusComponente.ATIVO);
    }

    public UnidadeMedidaResponse criarUnidade(String sigla, String nome, String status) {
        UnidadeMedida u = new UnidadeMedida();
        u.setId(sigla.trim().toUpperCase());
        exigirUnidadeLivre(sigla);
        u.setSigla(sigla.trim().toUpperCase());
        u.setNome((nome == null || nome.isBlank()) ? sigla.trim().toUpperCase() : nome.trim());
        u.setNomeNormalizado(TextoUtil.normalizar(u.getNome()));
        u.setOrigem("SISTEMA");
        u.setStatus(StatusComponente.normalizar(status));
        u.setCriadoPor(AuditoriaService.usuarioAtual());
        u.setCreatedAt(java.time.Instant.now());
        u.setUpdatedAt(java.time.Instant.now());
        UnidadeMedida salva = unidadeMedidaRepository.save(u);
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_CRIACAO_COMPONENTE, "UNIDADE_MEDIDA", salva.getId(), salva.getNome());
        log.info("[CATALOGO] Unidade criada: {}", salva.getId());
        return paraUnidade(salva);

    }

    public void exigirGrupoLivre(String nome) {
        if (grupoServicoRepository.existsById(DataSeeder.slug(nome))) {
            throw new ConflictException("Já existe um Grupo de Serviço com este nome (\"" + nome.trim()
                    + "\"). Reutilize o item existente.");
        }
    }

    /**
     * Regra de ouro: não existem dois Códigos de Serviço com o mesmo nome.
     * Qualquer tentativa de criar uma descrição já cadastrada (ativa ou não) é bloqueada —
     * o correto é reutilizar (ou reativar) o item existente.
     */
    public void exigirServicoLivre(String descricao) {
        exigirServicoLivre(descricao, null);
    }

    /** Mesma regra, ignorando um id (renomear o próprio item não pode colidir consigo mesmo). */
    public void exigirServicoLivre(String descricao, String ignorado) {
        if (descricao == null || descricao.isBlank()) {
            throw new BusinessException("A descrição do Código de Serviço é obrigatória.");
        }
        String normalizada = TextoUtil.normalizar(descricao);
        if (normalizada.isEmpty()) {
            throw new BusinessException("A descrição do Código de Serviço precisa ter letras ou números.");
        }
        boolean existeOutro = codigoServicoRepository.findByDescricaoNormalizada(normalizada).stream()
                .anyMatch(criado -> !criado.getId().equals(ignorado));
        if (existeOutro) {
            throw new ConflictException("Já existe um Código de Serviço com esta descrição (\"" + descricao.trim()
                    + "\"). Reutilize o item existente — não é permitido dois códigos com o mesmo nome.");
        }
    }

    /**
     * Marca um Código de Serviço aprovado apesar da similaridade como "caso isolado":
     * é uma exceção consciente (com motivo registrado), não um erro de cadastro.
     */
    public void marcarCasoIsolado(String csId, String motivo) {
        if (csId == null) {
            return;
        }
        codigoServicoRepository.findById(csId).ifPresent(cs -> {
            cs.setCasoIsolado(true);
            cs.setMotivoCasoIsolado(motivo == null || motivo.isBlank() ? null : motivo.trim());
            cs.setUpdatedAt(java.time.Instant.now());
            codigoServicoRepository.save(cs);
            similaridadeService.invalidarCache();
            log.info("[CATALOGO] Caso isolado marcado: {} ({})", cs.getId(), cs.getDescricao());
        });
    }

    public void exigirFornecedorLivre(String codigoSap) {
        if (codigoSap != null && !codigoSap.isBlank() && codigoFornecedorRepository.existsById(codigoSap.trim())) {
            throw new ConflictException("Já existe um fornecedor com o código " + codigoSap.trim()
                    + ". Reutilize o item existente.");
        }
    }

    public void exigirUnidadeLivre(String sigla) {
        if (sigla != null && unidadeMedidaRepository.existsById(sigla.trim().toUpperCase())) {
            throw new ConflictException("Já existe uma Unidade de Medida com a sigla "
                    + sigla.trim().toUpperCase() + ". Reutilize o item existente.");
        }
    }

    public void excluirGrupo(String id) {
        GrupoServico g = grupoServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de Serviço", id));
        exigirSemVinculos("o Grupo de Serviço", g.getNome(), codigoCompletoRepository.countByGsId(g.getId()));
        grupoServicoRepository.delete(g);
        concluirExclusao("GRUPO_SERVICO", g.getId(), g.getNome());
    }

    public void excluirFornecedor(String id) {
        CodigoFornecedor f = codigoFornecedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código de Fornecedor", id));
        exigirSemVinculos("o Código de Fornecedor", f.getNome(), codigoCompletoRepository.countByCfId(f.getId()));
        codigoFornecedorRepository.delete(f);
        concluirExclusao("CODIGO_FORNECEDOR", f.getId(), f.getNome());
    }

    public void excluirServico(String id) {
        CodigoServico cs = codigoServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código de Serviço", id));
        exigirSemVinculos("o Código de Serviço", cs.getDescricao(), codigoCompletoRepository.countByCsId(cs.getId()));
        codigoServicoRepository.delete(cs);
        concluirExclusao("CODIGO_SERVICO", cs.getId(), cs.getDescricao());
    }

    public void excluirUnidade(String id) {
        UnidadeMedida u = unidadeMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de Medida", id));
        exigirSemVinculos("a Unidade de Medida", u.getSigla(), codigoCompletoRepository.countByUmId(u.getId()));
        unidadeMedidaRepository.delete(u);
        concluirExclusao("UNIDADE_MEDIDA", u.getId(), u.getNome());
    }

    private void exigirSemVinculos(String rotulo, String nome, long vinculos) {
        if (vinculos > 0) {
            throw new ConflictException("Não é possível excluir " + rotulo + " \"" + nome + "\": está vinculado(a) a "
                    + vinculos + " código(s) completo(s). Exclua ou revise os vínculos antes.");
        }
    }

    private void concluirExclusao(String entityType, String id, String descricao) {
        similaridadeService.invalidarCache();
        auditar(EventoAuditoria.ACAO_EXCLUSAO_COMPONENTE, entityType, id, descricao);
        log.info("[CATALOGO] Excluído: {} {}", entityType, id);
    }

    private void auditar(String acao, String entityType, String entityId, String descricao) {
        auditoriaService.registrar(acao, entityType, entityId, "PERMITIR",
                "Cadastro de componente: " + descricao, java.util.List.of(), java.util.List.of(), null);
    }

    public List<CandidatoSimilarResponse> verificarDuplicidade(String texto, String tipo, int limite) {
        return similaridadeService.consultar(texto, limite, tipo).candidatos();
    }

    private String novoIdServico() {
        return sequenceService.proximoCodigoServico();
    }

    private CodigoServicoResponse servicoResponse(CodigoServico cs) {
        return new CodigoServicoResponse(cs.getId(), cs.getDescricao(), cs.getDescricaoNormalizada(),
                cs.getQuantidadeCaracteres(), cs.getOrigem(),
                cs.getLegacyIds() == null ? List.of() : cs.getLegacyIds(), StatusComponente.normalizar(cs.getStatus()),
                cs.getCreatedAt(), cs.getCriadoPor(),
                Boolean.TRUE.equals(cs.getCasoIsolado()), cs.getMotivoCasoIsolado());
    }

    private static <T> List<T> apenasAtivos(List<T> itens, boolean incluirTodos,
                                            java.util.function.Function<T, String> status) {
        if (incluirTodos) {
            return itens;
        }
        return itens.stream().filter(i -> StatusComponente.ativo(status.apply(i))).toList();
    }

    /**
     * Tira (ou devolve) um Código de Serviço da lista ativa — soft delete:
     * nada é apagado; vínculos, histórico e auditoria continuam.
     */
    public void definirStatusServico(String csId, String novoStatus) {
        if (csId == null) {
            return;
        }
        codigoServicoRepository.findById(csId).ifPresent(cs -> {
            String status = StatusComponente.normalizar(novoStatus);
            cs.setStatus(status);
            cs.setUpdatedAt(java.time.Instant.now());
            codigoServicoRepository.save(cs);
            similaridadeService.invalidarCache();
            boolean desativando = StatusComponente.DESATIVADO.equals(status);
            auditar(desativando ? EventoAuditoria.ACAO_DESATIVACAO_COMPONENTE
                            : EventoAuditoria.ACAO_REATIVACAO_COMPONENTE,
                    "CODIGO_SERVICO", cs.getId(), cs.getDescricao());
            log.info("[CATALOGO] Status do serviço {} alterado para {}", cs.getId(), status);
        });
    }

    /**
     * Altera a descrição de um Código de Serviço que ainda está em validação
     * (pertencente a um pedido). Códigos já em circulação não podem ser renomeados
     * por aqui — podem ser compartilhados por outros vínculos.
     */
    public void atualizarDescricaoServico(String csId, String descricao) {
        CodigoServico cs = codigoServicoRepository.findById(csId)
                .orElseThrow(() -> new BusinessException("Código de Serviço do pedido não encontrado."));
        if (!StatusComponente.PENDENTE_VALIDACAO.equals(cs.getStatus())) {
            throw new BusinessException("A descrição não pode ser alterada: este código de serviço já está "
                    + "cadastrado e pode ser usado por outros vínculos. Para mudanças, acione a governança.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new BusinessException("A nova descrição do serviço é obrigatória.");
        }
        if (TextoUtil.excedeLimiteSap(descricao.trim())) {
            throw new BusinessException("A descrição do serviço não pode exceder "
                    + TextoUtil.LIMITE_SAP + " caracteres (regra SAP).");
        }
        exigirServicoLivre(descricao, csId);
        cs.setDescricao(descricao.trim());
        cs.setDescricaoNormalizada(TextoUtil.normalizar(descricao));
        cs.setQuantidadeCaracteres(descricao.trim().length());
        cs.setUpdatedAt(java.time.Instant.now());
        codigoServicoRepository.save(cs);
        similaridadeService.invalidarCache();
        log.info("[CATALOGO] Descrição do serviço {} atualizada: {}", cs.getId(), cs.getDescricao());
    }

    /**
     * Ao cancelar um pedido, os componentes que nasceram com ele (ainda em validação)
     * e não são usados por nenhum outro pedido vivo não podem ficar órfãos: saem de cena.
     * Componentes compartilhados com outros pedidos continuam — serão decididos por eles.
     */
    public void desativarPendentesNaoUsados(CodigoCompleto codigo,
                                            boolean gsEmUso, boolean cfEmUso, boolean csEmUso, boolean umEmUso) {
        if (!gsEmUso) {
            grupoServicoRepository.findById(codigo.getGsId()).ifPresent(g -> {
                if (pendente(g.getStatus())) {
                    g.setStatus(StatusComponente.DESATIVADO);
                    g.setUpdatedAt(java.time.Instant.now());
                    grupoServicoRepository.save(g);
                }
            });
        }
        if (!cfEmUso) {
            codigoFornecedorRepository.findById(codigo.getCfId()).ifPresent(f -> {
                if (pendente(f.getStatus())) {
                    f.setStatus(StatusComponente.DESATIVADO);
                    f.setUpdatedAt(java.time.Instant.now());
                    codigoFornecedorRepository.save(f);
                }
            });
        }
        if (!csEmUso) {
            codigoServicoRepository.findById(codigo.getCsId()).ifPresent(cs -> {
                if (pendente(cs.getStatus())) {
                    cs.setStatus(StatusComponente.DESATIVADO);
                    cs.setUpdatedAt(java.time.Instant.now());
                    codigoServicoRepository.save(cs);
                    similaridadeService.invalidarCache();
                }
            });
        }
        if (!umEmUso) {
            unidadeMedidaRepository.findById(codigo.getUmId()).ifPresent(u -> {
                if (pendente(u.getStatus())) {
                    u.setStatus(StatusComponente.DESATIVADO);
                    u.setUpdatedAt(java.time.Instant.now());
                    unidadeMedidaRepository.save(u);
                }
            });
        }
    }

    /**
     * Marca itens com o badge de caso especial (!) e o motivo exibido no hover.
     * Usado pela varredura de duplicidades e por exceções aprovadas.
     *
     * Ciclo de vida do caso especial (regra de negócio apresentada):
     *   1. MARCAR  — humano marca pelo catálogo (motivo obrigatório) ou a governança marca
     *                ao aprovar algo com similaridade / ao unificar duplicidades;
     *   2. REVISAR — o item aparece no filtro "Casos especiais" e o hover explica o motivo;
     *   3. RESOLVER (limparCasosIsolados) ou EXCLUIR (com transferência de vínculos).
     */
    public java.util.Map<String, Object> marcarCasosIsolados(List<String> ids, String motivo) {
        return marcarCasosIsolados(ids, motivo, null);
    }

    public java.util.Map<String, Object> marcarCasosIsolados(List<String> ids, String motivo,
                                                             java.util.Map<String, String> motivos) {
        boolean temGeral = motivo != null && !motivo.isBlank();
        if (!temGeral && (motivos == null || motivos.isEmpty())) {
            throw new BusinessException("Informe o motivo do caso especial — ele aparece no hover do badge (!).");
        }
        int marcados = 0;
        for (String id : ids == null ? List.<String>of() : ids) {
            String texto = motivos == null ? null : motivos.get(id);
            marcarCasoIsolado(id, texto != null && !texto.isBlank() ? texto : motivo.trim());
            marcados++;
        }
        return java.util.Map.of("marcados", marcados);
    }

    /** Remove a marcação (!) dos itens informados. */
    public java.util.Map<String, Object> limparCasosIsolados(List<String> ids) {
        int limpos = 0;
        for (String id : ids == null ? List.<String>of() : ids) {
            if (codigoServicoRepository.findById(id).isPresent()) {
                CodigoServico cs = codigoServicoRepository.findById(id).orElseThrow();
                cs.setCasoIsolado(false);
                cs.setMotivoCasoIsolado(null);
                cs.setUpdatedAt(java.time.Instant.now());
                codigoServicoRepository.save(cs);
                limpos++;
            }
        }
        similaridadeService.invalidarCache();
        return java.util.Map.of("limpos", limpos);
    }

    /**
     * Desativa (soft delete) vários serviços de uma vez — usado pela varredura de duplicidades.
     */
    public int desativarServicos(List<String> ids) {
        int desativados = 0;
        for (String id : ids == null ? List.<String>of() : ids) {
            if (codigoServicoRepository.findById(id).isPresent()) {
                definirStatusServico(id, StatusComponente.DESATIVADO);
                desativados++;
            }
        }
        return desativados;
    }

    public void definirStatusComponentes(CodigoCompleto codigo, String novoStatus) {
        String alvo = StatusComponente.normalizar(novoStatus);
        grupoServicoRepository.findById(codigo.getGsId()).ifPresent(g -> {
            if (pendente(g.getStatus())) {
                g.setStatus(alvo);
                g.setUpdatedAt(java.time.Instant.now());
                grupoServicoRepository.save(g);
            }
        });
        codigoFornecedorRepository.findById(codigo.getCfId()).ifPresent(f -> {
            if (pendente(f.getStatus())) {
                f.setStatus(alvo);
                f.setUpdatedAt(java.time.Instant.now());
                codigoFornecedorRepository.save(f);
            }
        });
        codigoServicoRepository.findById(codigo.getCsId()).ifPresent(cs -> {
            if (pendente(cs.getStatus())) {
                cs.setStatus(alvo);
                cs.setUpdatedAt(java.time.Instant.now());
                codigoServicoRepository.save(cs);
            }
        });
        unidadeMedidaRepository.findById(codigo.getUmId()).ifPresent(u -> {
            if (pendente(u.getStatus())) {
                u.setStatus(alvo);
                u.setUpdatedAt(java.time.Instant.now());
                unidadeMedidaRepository.save(u);
            }
        });
    }

    private static boolean pendente(String status) {
        return StatusComponente.PENDENTE_VALIDACAO.equals(StatusComponente.normalizar(status));
    }

    /** Quantidade de composições (códigos completos) que referenciam o componente. */
    public long contarVinculos(String tipo, String id) {
        return switch (tipo == null ? "" : tipo.trim().toUpperCase()) {
            case "GS" -> codigoCompletoRepository.countByGsId(id);
            case "CF" -> codigoCompletoRepository.countByCfId(id);
            case "CS" -> codigoCompletoRepository.countByCsId(id);
            case "UM" -> codigoCompletoRepository.countByUmId(id);
            default -> 0;
        };
    }

    /**
     * Transfere os vínculos de um componente para outro (da mesma natureza) e, quando
     * possível, exclui o de origem — é o caminho para deduplicar itens que têm vínculos.
     *
     * Semântica de mesclagem (por que "composição já existente" não é erro):
     *   - composição que já existe no destino ⇒ o vínculo de origem é REDUNDANTE;
     *     ele é removido (mesclagem) e o destino continua valendo;
     *   - composição que não existe ⇒ o documento é recriado com o novo id de composição
     *     (o _id do Mongo é imutável), preservando status, justificativas e histórico;
     *   - no fim, sem vínculos restantes, o item de origem pode ser excluído de verdade.
     * Tudo é auditado (TRANSFERENCIA_VINCULOS / MESCLAGEM).
     */
    public java.util.Map<String, Object> transferirVinculos(String tipo, String de, String para, boolean excluirOrigem) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase();
        if (de == null || de.isBlank() || para == null || para.isBlank() || de.equals(para)) {
            throw new BusinessException("Informe o item de origem e um item de destino diferente.");
        }
        boolean destinoExiste = switch (t) {
            case "GS" -> grupoServicoRepository.findById(para).isPresent();
            case "CF" -> codigoFornecedorRepository.findById(para).isPresent();
            case "CS" -> codigoServicoRepository.findById(para).isPresent();
            case "UM" -> unidadeMedidaRepository.findById(para).isPresent();
            default -> false;
        };
        if (!destinoExiste) {
            throw new ResourceNotFoundException("Componente de destino", para);
        }
        List<CodigoCompleto> vinculos = switch (t) {
            case "GS" -> codigoCompletoRepository.findByGsId(de);
            case "CF" -> codigoCompletoRepository.findByCfId(de);
            case "CS" -> codigoCompletoRepository.findByCsId(de);
            case "UM" -> codigoCompletoRepository.findByUmId(de);
            default -> throw new BusinessException("Tipo de componente desconhecido: " + tipo);
        };

        int transferidos = 0;
        int mesclados = 0;
        List<String> problemas = new ArrayList<>();
        for (CodigoCompleto c : vinculos) {
            String novoId = ("GS".equals(t) ? para : c.getGsId()) + "_" + ("CF".equals(t) ? para : c.getCfId())
                    + "_" + ("CS".equals(t) ? para : c.getCsId()) + "_" + ("UM".equals(t) ? para : c.getUmId());
            // a composição de destino pode ter vários registros (inclusive com sufixo #N):
            // o vínculo só é redundante se já existir OUTRO registro com essa composição
            CodigoCompleto existente = codigoCompletoRepository.findByIdCompletoOrderByCreatedAtDesc(novoId).stream()
                    .filter(outro -> !outro.getId().equals(c.getId()))
                    .findFirst().orElse(null);
            if (existente != null) {
                // a composição já existe no destino: o vínculo de origem é REDUNDANTE —
                // em vez de travar, ele é removido (mesclagem) e o destino continua valendo
                codigoCompletoRepository.delete(c);
                mesclados++;
                auditoriaService.registrar(EventoAuditoria.ACAO_TRANSFERENCIA_VINCULOS, "CODIGO_COMPLETO",
                        novoId, "MESCLAGEM", "Composicao redundante " + c.getIdCompleto()
                                + " removida (ja existia como " + novoId + ") ao transferir de " + de + " para " + para,
                        List.of(de, para), List.of(), AuditoriaService.usuarioAtual());
                continue;
            }
            CodigoCompleto novo = copiarVinculo(c, t, para, novoId);
            codigoCompletoRepository.delete(c);
            codigoCompletoRepository.save(novo);
            transferidos++;
            auditoriaService.registrar(EventoAuditoria.ACAO_TRANSFERENCIA_VINCULOS, "CODIGO_COMPLETO",
                    novoId, "TRANSFERENCIA", "Vinculo transferido de " + de + " para " + para,
                    List.of(de, para), List.of(), AuditoriaService.usuarioAtual());
        }

        boolean excluido = false;
        if (excluirOrigem) {
            switch (t) {
                case "GS" -> excluirGrupo(de);
                case "CF" -> excluirFornecedor(de);
                case "CS" -> excluirServico(de);
                case "UM" -> excluirUnidade(de);
                default -> { }
            }
            excluido = true;
        }
        return java.util.Map.of("transferidos", transferidos, "mesclados", mesclados,
                "conflitos", problemas, "excluido", excluido);
    }

    private CodigoCompleto copiarVinculo(CodigoCompleto c, String tipo, String para, String novoId) {
        CodigoCompleto n = new CodigoCompleto();
        n.setId(novoId);
        n.setIdCompleto(novoId);
        n.setGsId("GS".equals(tipo) ? para : c.getGsId());
        n.setCfId("CF".equals(tipo) ? para : c.getCfId());
        n.setCsId("CS".equals(tipo) ? para : c.getCsId());
        n.setUmId("UM".equals(tipo) ? para : c.getUmId());
        n.setDescricaoSnapshot(c.getDescricaoSnapshot());
        n.setUsuarioCriacaoId(c.getUsuarioCriacaoId());
        n.setCreatedAt(c.getCreatedAt());
        n.setOrigem(c.getOrigem());
        n.setLegacyIds(c.getLegacyIds());
        n.setStatus(c.getStatus());
        n.setDecisaoSimilaridade(c.getDecisaoSimilaridade());
        n.setMelhorScore(c.getMelhorScore());
        n.setJustificativa(c.getJustificativa());
        n.setSimilaresRegistrados(c.getSimilaresRegistrados());
        n.setAprovadoPor(c.getAprovadoPor());
        n.setAprovadoEm(c.getAprovadoEm());
        n.setObservacaoAprovacao(c.getObservacaoAprovacao());
        // a Idempotency-Key pertence à requisição que criou o vínculo original —
        // copiá-la deixaria a chave "presa" a outra composição e faria um novo envio
        // da composição antiga ser respondido como se tivesse sido criado (sem criar nada)
        n.setIdempotencyKey(null);
        n.setIdempotencyHash(null);
        return n;
    }

    public List<java.util.Map<String, Object>> excluirEmMassa(String tipo, List<String> ids) {
        List<java.util.Map<String, Object>> relatorio = new ArrayList<>();
        for (String id : ids == null ? List.<String>of() : ids) {
            java.util.Map<String, Object> linha = new java.util.LinkedHashMap<>();
            linha.put("id", id);
            try {
                switch (tipo == null ? "" : tipo.trim().toUpperCase()) {
                    case "GS" -> excluirGrupo(id);
                    case "CF" -> excluirFornecedor(id);
                    case "CS" -> excluirServico(id);
                    case "UM" -> excluirUnidade(id);
                    default -> throw new IllegalArgumentException("Tipo de componente desconhecido: " + tipo);
                }
                linha.put("excluido", true);
                linha.put("motivo", "Excluido com sucesso.");
            } catch (Exception e) {
                linha.put("excluido", false);
                linha.put("motivo", e.getMessage() == null ? "Nao foi possivel excluir." : e.getMessage());
            }
            relatorio.add(linha);
        }
        return relatorio;
    }

    public List<java.util.Map<String, Object>> definirSituacaoEmMassa(String tipo, List<String> ids, String situacao) {
        String alvo = situacao == null ? "" : situacao.trim().toUpperCase();
        if (!StatusComponente.DESATIVADO.equals(alvo) && !StatusComponente.ATIVO.equals(alvo)) {
            throw new BusinessException("Informe a situacao desejada: DESATIVADO ou ATIVO.");
        }
        boolean desativar = StatusComponente.DESATIVADO.equals(alvo);
        String entityType = switch (tipo == null ? "" : tipo.trim().toUpperCase()) {
            case "GS" -> "GRUPO_SERVICO";
            case "CF" -> "CODIGO_FORNECEDOR";
            case "CS" -> "CODIGO_SERVICO";
            case "UM" -> "UNIDADE_MEDIDA";
            default -> throw new BusinessException("Tipo de componente desconhecido: " + tipo);
        };

        List<java.util.Map<String, Object>> relatorio = new ArrayList<>();
        for (String id : ids == null ? List.<String>of() : ids) {
            java.util.Map<String, Object> linha = new java.util.LinkedHashMap<>();
            linha.put("id", id);
            try {
                String rotulo = alterarSituacao(tipo.trim().toUpperCase(), id, alvo);
                linha.put("alterado", true);
                linha.put("motivo", desativar
                        ? "Desativado — sai de circulacao e pode ser reativado a qualquer momento."
                        : "Reativado — de volta as listas de uso.");
                auditoriaService.registrar(
                        desativar ? EventoAuditoria.ACAO_DESATIVACAO_COMPONENTE : EventoAuditoria.ACAO_REATIVACAO_COMPONENTE,
                        entityType, id, "PERMITIR",
                        (desativar ? "Componente desativado: " : "Componente reativado: ") + rotulo,
                        List.of(), List.of(), null);
                log.info("[CATALOGO] {} {} {}", desativar ? "Desativado" : "Reativado", entityType, id);
            } catch (Exception e) {
                linha.put("alterado", false);
                linha.put("motivo", e.getMessage() == null ? "Nao foi possivel alterar a situacao." : e.getMessage());
            }
            relatorio.add(linha);
        }
        return relatorio;
    }

    private String alterarSituacao(String tipo, String id, String alvo) {
        switch (tipo) {
            case "GS": {
                GrupoServico g = grupoServicoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Grupo de Servi\u00e7o", id));
                validarTransicao(g.getStatus(), alvo);
                g.setStatus(alvo);
                g.setActive(StatusComponente.ativo(alvo));
                g.setUpdatedAt(java.time.Instant.now());
                grupoServicoRepository.save(g);
                return g.getNome();
            }
            case "CF": {
                CodigoFornecedor f = codigoFornecedorRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("C\u00f3digo de Fornecedor", id));
                validarTransicao(f.getStatus(), alvo);
                f.setStatus(alvo);
                f.setActive(StatusComponente.ativo(alvo));
                f.setUpdatedAt(java.time.Instant.now());
                codigoFornecedorRepository.save(f);
                return f.getNome();
            }
            case "CS": {
                CodigoServico cs = codigoServicoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("C\u00f3digo de Servi\u00e7o", id));
                validarTransicao(cs.getStatus(), alvo);
                cs.setStatus(alvo);
                cs.setActive(StatusComponente.ativo(alvo));
                cs.setUpdatedAt(java.time.Instant.now());
                codigoServicoRepository.save(cs);
                return cs.getDescricao();
            }
            case "UM": {
                UnidadeMedida u = unidadeMedidaRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Unidade de Medida", id));
                validarTransicao(u.getStatus(), alvo);
                u.setStatus(alvo);
                u.setActive(StatusComponente.ativo(alvo));
                u.setUpdatedAt(java.time.Instant.now());
                unidadeMedidaRepository.save(u);
                return u.getSigla();
            }
            default:
                throw new BusinessException("Tipo de componente desconhecido: " + tipo);
        }
    }

    private static void validarTransicao(String atual, String alvo) {
        String situacao = StatusComponente.normalizar(atual);
        if (StatusComponente.PENDENTE_VALIDACAO.equals(situacao)) {
            throw new BusinessException("O item est\u00e1 em aprova\u00e7\u00e3o — quem decide \u00e9 a Governan\u00e7a.");
        }
        if (StatusComponente.REJEITADO.equals(situacao) && StatusComponente.ATIVO.equals(alvo)) {
            throw new BusinessException("O item foi rejeitado pela Governan\u00e7a — revise a decis\u00e3o por l\u00e1.");
        }
        if (situacao.equals(StatusComponente.normalizar(alvo))) {
            throw new BusinessException(StatusComponente.ATIVO.equals(alvo)
                    ? "O item j\u00e1 est\u00e1 ativo."
                    : "O item j\u00e1 estava desativado.");
        }
    }

    public java.util.Map<String, Object> importarEmMassa(String tipo,
            List<ImportacaoCatalogoRequest.ItemImportacao> itens) {
        String alvo = tipo == null ? "" : tipo.trim().toUpperCase();
        List<java.util.Map<String, Object>> ignorados = new ArrayList<>();
        List<String> idsAdicionados = new ArrayList<>();
        int linha = 0;
        for (ImportacaoCatalogoRequest.ItemImportacao item : itens == null ? List.<ImportacaoCatalogoRequest.ItemImportacao>of() : itens) {
            linha++;
            String rotulo = rotuloItem(item);
            try {
                switch (alvo) {
                    case "GS" -> idsAdicionados.add(importarGrupo(item));
                    case "CF" -> idsAdicionados.add(importarFornecedor(item));
                    case "CS" -> idsAdicionados.add(importarServico(item));
                    case "UM" -> idsAdicionados.add(importarUnidade(item));
                    default -> throw new IllegalArgumentException("Tipo de componente desconhecido: " + tipo);
                }
            } catch (Exception e) {
                java.util.Map<String, Object> falha = new java.util.LinkedHashMap<>();
                falha.put("linha", linha);
                falha.put("item", rotulo);
                falha.put("motivo", e.getMessage() == null ? "Nao foi possivel importar." : e.getMessage());
                ignorados.add(falha);
            }
        }
        java.util.Map<String, Object> relatorio = new java.util.LinkedHashMap<>();
        relatorio.put("tipo", alvo);
        relatorio.put("total", linha);
        relatorio.put("adicionados", idsAdicionados.size());
        relatorio.put("idsAdicionados", idsAdicionados);
        relatorio.put("ignorados", ignorados);
        log.info("[CATALOGO] Importacao {}: {} linha(s), {} adicionada(s), {} ignorada(s)",
                alvo, linha, idsAdicionados.size(), ignorados.size());
        return relatorio;
    }

    private String importarGrupo(ImportacaoCatalogoRequest.ItemImportacao item) {
        String nome = texto(item.nome());
        if (nome.isEmpty()) {
            throw new BusinessException("Informe a coluna 'nome'.");
        }
        if (grupoServicoRepository.findById(DataSeeder.slug(nome)).isPresent()
                || !grupoServicoRepository.findByNomeNormalizado(TextoUtil.normalizar(nome)).isEmpty()) {
            throw new ConflictException("Ja existe Grupo de Servico com este nome — o item atual foi mantido.");
        }
        return criarGrupo(nome, StatusComponente.ATIVO).id();
    }

    private String importarFornecedor(ImportacaoCatalogoRequest.ItemImportacao item) {
        String nome = texto(item.nome());
        String sap = texto(item.codigoSap()).isEmpty() ? texto(item.id()) : texto(item.codigoSap());
        if (nome.isEmpty()) {
            throw new BusinessException("Informe a coluna 'nome'.");
        }
        if (!sap.isEmpty() && codigoFornecedorRepository.existsById(sap)) {
            throw new ConflictException("Ja existe Codigo de Fornecedor com o codigo " + sap
                    + " — o item atual foi mantido.");
        }
        if (sap.isEmpty() && !codigoFornecedorRepository.findByNomeNormalizado(TextoUtil.normalizar(nome)).isEmpty()) {
            throw new ConflictException("Ja existe Codigo de Fornecedor com este nome — o item atual foi mantido.");
        }
        return criarFornecedor(nome, sap.isEmpty() ? null : sap, StatusComponente.ATIVO).id();
    }

    private String importarServico(ImportacaoCatalogoRequest.ItemImportacao item) {
        String descricao = texto(item.descricao());
        if (descricao.isEmpty()) {
            throw new BusinessException("Informe a coluna 'descricao'.");
        }
        if (descricao.length() > TextoUtil.LIMITE_SAP) {
            throw new BusinessException("Descricao com " + descricao.length() + " caracteres — o limite SAP e "
                    + TextoUtil.LIMITE_SAP + ".");
        }
        if (!codigoServicoRepository.findByDescricaoNormalizada(TextoUtil.normalizar(descricao)).isEmpty()) {
            throw new ConflictException("Ja existe Codigo de Servico com esta descricao — o item atual foi mantido.");
        }
        return criarServico(descricao, StatusComponente.ATIVO).id();
    }

    private String importarUnidade(ImportacaoCatalogoRequest.ItemImportacao item) {
        String sigla = texto(item.sigla()).isEmpty() ? texto(item.id()) : texto(item.sigla());
        if (sigla.isEmpty()) {
            throw new BusinessException("Informe a coluna 'sigla'.");
        }
        String nome = texto(item.nome()).isEmpty() ? sigla : texto(item.nome());
        if (unidadeMedidaRepository.existsById(sigla.trim().toUpperCase())) {
            throw new ConflictException("Ja existe Unidade de Medida com a sigla "
                    + sigla.trim().toUpperCase() + " — a item atual foi mantida.");
        }
        return criarUnidade(sigla, nome, StatusComponente.ATIVO).id();
    }

    private static String rotuloItem(ImportacaoCatalogoRequest.ItemImportacao item) {
        if (item == null) {
            return "";
        }
        String rotulo = texto(item.nome());
        if (rotulo.isEmpty()) rotulo = texto(item.descricao());
        if (rotulo.isEmpty()) rotulo = texto(item.sigla());
        if (rotulo.isEmpty()) rotulo = texto(item.codigoSap());
        return rotulo;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static GrupoServicoResponse paraGrupo(GrupoServico g) {
        return new GrupoServicoResponse(g.getId(), g.getNome(), g.getNomeNormalizado(), g.getOrigem(),
                StatusComponente.normalizar(g.getStatus()), g.getCreatedAt(), g.getCriadoPor());
    }

    private static CodigoFornecedorResponse paraFornecedor(CodigoFornecedor f) {
        return new CodigoFornecedorResponse(f.getId(), f.getCodigoFornecedorSAP(), f.getNome(),
                f.getNomeNormalizado(), f.getOrigem(), StatusComponente.normalizar(f.getStatus()),
                f.getCreatedAt(), f.getCriadoPor());
    }

    private static UnidadeMedidaResponse paraUnidade(UnidadeMedida u) {
        return new UnidadeMedidaResponse(u.getId(), u.getSigla(), u.getNome(), u.getNomeNormalizado(),
                u.getOrigem(), StatusComponente.normalizar(u.getStatus()), u.getCreatedAt(), u.getCriadoPor());
    }

    private static String regex(String termo) {
        return com.gerdau.desafio.util.TextoUtil.padraoBusca(termo);
    }
}
