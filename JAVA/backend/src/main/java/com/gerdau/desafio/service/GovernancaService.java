package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.GovernancaDecisaoRequest;
import com.gerdau.desafio.dto.request.GovernancaReutilizarRequest;
import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.dto.request.RevisaoMatrizRequest;
import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.dto.response.ExcecaoResponse;
import com.gerdau.desafio.dto.response.ItemGovernancaResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.EventoAuditoria;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.MatrizDePara;
import com.gerdau.desafio.model.RegistroSimilar;
import com.gerdau.desafio.model.StatusComponente;
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GovernancaService {

    private static final Logger log = LoggerFactory.getLogger(GovernancaService.class);

    private final MatrizDeParaRepository matrizDeParaRepository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final ExcecaoRepository excecaoRepository;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final AuditoriaService auditoriaService;
    private final CatalogoService catalogoService;
    private final PreviewService previewService;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public GovernancaService(MatrizDeParaRepository matrizDeParaRepository,
                             CodigoCompletoRepository codigoCompletoRepository,
                             ExcecaoRepository excecaoRepository,
                             EventoAuditoriaRepository eventoAuditoriaRepository,
                             AuditoriaService auditoriaService,
                             CatalogoService catalogoService,
                             PreviewService previewService,
                             org.springframework.data.mongodb.core.MongoTemplate mongoTemplate) {
        this.matrizDeParaRepository = matrizDeParaRepository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.excecaoRepository = excecaoRepository;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.auditoriaService = auditoriaService;
        this.catalogoService = catalogoService;
        this.previewService = previewService;
        this.mongoTemplate = mongoTemplate;
    }

    public List<ItemGovernancaResponse> listarPendentes(String origem, String query, int limite,
                                                       String status, String decision, int pagina) {
        List<ItemGovernancaResponse> itens = new ArrayList<>();
        boolean incluiHistorico = origem == null || origem.isBlank()
                || "TODOS".equalsIgnoreCase(origem) || "HISTORICO".equalsIgnoreCase(origem);

        String decisao = (decision == null || decision.isBlank() || "TODOS".equalsIgnoreCase(decision))
                ? null : decision.trim().toUpperCase();
        boolean incluiNovos = (origem == null || origem.isBlank()
                || "TODOS".equalsIgnoreCase(origem) || "NOVOS".equalsIgnoreCase(origem))
                && decisao == null;

        String statusMatriz = (status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status))
                ? MatrizDePara.STATUS_PENDENTE : status.trim().toUpperCase();
        Pageable paginacao = PageRequest.of(Math.max(pagina, 0), limite);

        if (incluiHistorico) {
            List<MatrizDePara> matriz;
            if (query != null && !query.isBlank()) {
                matriz = matrizDeParaRepository.buscarPorTexto(TextoUtil.padraoBusca(query), PageRequest.of(0, limite)).stream()
                        .filter(m -> statusMatriz.equals(m.getStatusRevisao()))
                        .filter(m -> decisao == null || decisao.equals(m.getDecision()))
                        .limit(limite).toList();
            } else if (decisao != null) {
                matriz = matrizDeParaRepository.findByDecisionAndStatusRevisaoOrderByScoreDesc(
                        decisao, statusMatriz, paginacao);
            } else {
                matriz = matrizDeParaRepository.findByStatusRevisaoOrderByScoreDesc(statusMatriz, paginacao);
            }
            matriz.forEach(m -> itens.add(paraItemHistorico(m)));
        }

        if (incluiNovos && (status == null || status.isBlank()
                || "TODOS".equalsIgnoreCase(status) || "PENDENTE".equalsIgnoreCase(status))) {
            codigoCompletoRepository.findByStatusOrderByCreatedAtDesc(CodigoCompleto.STATUS_PENDENTE)
                    .stream()
                    .skip((long) Math.max(pagina, 0) * limite)
                    .limit(limite)
                    .forEach(c -> itens.add(paraItemNovo(c)));
        }
        return itens;
    }

    public Map<String, Object> resumo() {
        long pendentesHistorico = matrizDeParaRepository.countByStatusRevisao(MatrizDePara.STATUS_PENDENTE);
        long revisados = matrizDeParaRepository.countByStatusRevisao(MatrizDePara.STATUS_REVISADO);
        long duplicidades = matrizDeParaRepository.countByDecision("DUPLICIDADE");
        long alertas = matrizDeParaRepository.countByDecision("ALERTA");
        long originais = matrizDeParaRepository.countByDecision("ORIGINAL");
        long pendentesNovos = codigoCompletoRepository
                .findByStatusOrderByCreatedAtDesc(CodigoCompleto.STATUS_PENDENTE).size();
        return Map.of(
                "pendentesHistorico", pendentesHistorico,
                "revisados", revisados,
                "decisionDuplicidade", duplicidades,
                "decisionAlerta", alertas,
                "decisionOriginal", originais,
                "pendentesNovos", pendentesNovos,
                "excecoes", excecaoRepository.count(),
                "totalLegados", matrizDeParaRepository.count(),
                "grupos", contagemPorGrupo());
    }

    private List<Map<String, Object>> contagemPorGrupo() {
        List<Map<String, Object>> grupos = new ArrayList<>();
        for (String nome : mongoTemplate.findDistinct(
                new org.springframework.data.mongodb.core.query.Query(), "grupoServico",
                MatrizDePara.class, String.class)) {
            grupos.add(Map.of("nome", nome, "quantidade", matrizDeParaRepository.countByGrupoServico(nome)));
        }
        grupos.sort((a, b) -> Long.compare((Long) b.get("quantidade"), (Long) a.get("quantidade")));
        return grupos;
    }

    public ItemGovernancaResponse revisarHistorico(String id, RevisaoMatrizRequest request) {
        MatrizDePara doc = matrizDeParaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro da matriz", id));
        String acao = request.acao() == null ? "" : request.acao().trim().toUpperCase(java.util.Locale.ROOT);
        if (!"MANTER".equals(acao) && !"EXCLUIR".equals(acao)) {
            throw new BusinessException("Ação inválida ('" + request.acao()
                    + "'): use MANTER (aceitar) ou EXCLUIR (apagar do catálogo).");
        }
        if (!MatrizDePara.STATUS_PENDENTE.equals(doc.getStatusRevisao())) {
            throw new BusinessException("Este item já foi decidido (status: " + doc.getStatusRevisao()
                    + ") — para reabrir, use a reversão da decisão.");
        }
        String motivo = request.observacao() == null ? "" : request.observacao().trim();
        if (motivo.isBlank()) {
            throw new BusinessException("Informe o motivo da decisão — ele é obrigatório.");
        }
        doc.setStatusRevisao(MatrizDePara.STATUS_REVISADO);
        doc.setReviewedBy(request.revisadoPor());
        doc.setReviewedAt(Instant.now());
        doc.setObservacaoRevisao((request.acao() == null ? "" : request.acao() + " — ") + motivo);
        MatrizDePara salvo = matrizDeParaRepository.save(doc);

        boolean aceitouRepeticao = "MANTER".equalsIgnoreCase(request.acao())
                && ("DUPLICIDADE".equals(doc.getDecision()) || "ALERTA".equals(doc.getDecision()));
        if (aceitouRepeticao) {
            catalogoService.marcarCasoIsolado(doc.getNewId(), motivo);
        }
        if ("EXCLUIR".equalsIgnoreCase(request.acao())) {
            catalogoService.definirStatusServico(doc.getNewId(), StatusComponente.DESATIVADO);
        }
        auditoriaService.registrar(EventoAuditoria.ACAO_REVISAO_HISTORICA, "MATRIZ_DE_PARA",
                salvo.getNewId(), salvo.getDecision(),
                doc.getObservacaoRevisao(),
                java.util.List.of(salvo.getNewId()),
                java.util.List.of(salvo.getScore() == null ? 0.0 : salvo.getScore()),
                request.revisadoPor());
        log.info("[GOVERNANCA] Histórico revisado: newId={} por {}", salvo.getNewId(), request.revisadoPor());
        return paraItemHistorico(salvo);
    }

    public ItemGovernancaResponse aprovarNovo(String id, GovernancaDecisaoRequest request) {
        CodigoCompleto model = exigirPendenteNovo(id);

        boolean casoIsolado = model.getDecisaoSimilaridade() != null
                && !"PERMITIR".equals(model.getDecisaoSimilaridade());
        String motivo = request.observacao() == null ? "" : request.observacao().trim();
        if (casoIsolado && motivo.isEmpty()) {
            throw new BusinessException("Aprovar um cadastro com similaridade (" + model.getDecisaoSimilaridade()
                    + ") exige justificativa: descreva o motivo da exceção à política de similaridade.");
        }

        model.setStatus(CodigoCompleto.STATUS_ATIVO);
        catalogoService.definirStatusComponentes(model, StatusComponente.ATIVO);
        model.setAprovadoPor(request.aprovadoPor());
        model.setObservacaoAprovacao(request.observacao());
        model.setAprovadoEm(Instant.now());
        if (casoIsolado) {
            catalogoService.marcarCasoIsolado(model.getCsId(), motivo);
        }
        CodigoCompleto salvo = codigoCompletoRepository.save(model);
        auditoriaService.registrar(EventoAuditoria.ACAO_APROVACAO, "CODIGO_COMPLETO",
                salvo.getIdCompleto(), salvo.getDecisaoSimilaridade(), request.observacao(),
                java.util.List.of(), java.util.List.of(), request.aprovadoPor());
        log.info("[GOVERNANCA] Aprovado: idCompleto={} por {}", salvo.getIdCompleto(), request.aprovadoPor());
        return paraItemNovo(salvo);
    }

    public ItemGovernancaResponse rejeitarNovo(String id, GovernancaDecisaoRequest request) {
        CodigoCompleto model = exigirPendenteNovo(id);
        model.setStatus(CodigoCompleto.STATUS_REJEITADO);
        catalogoService.definirStatusComponentes(model, StatusComponente.REJEITADO);
        model.setAprovadoPor(request.aprovadoPor());
        model.setObservacaoAprovacao(request.observacao());
        model.setAprovadoEm(Instant.now());
        model.setIdempotencyKey(null);
        model.setIdempotencyHash(null);
        CodigoCompleto salvo = codigoCompletoRepository.save(model);
        auditoriaService.registrar(EventoAuditoria.ACAO_REJEICAO, "CODIGO_COMPLETO",
                salvo.getIdCompleto(), salvo.getDecisaoSimilaridade(), request.observacao(),
                java.util.List.of(), java.util.List.of(), request.aprovadoPor());
        log.info("[GOVERNANCA] Rejeitado: idCompleto={} por {}", salvo.getIdCompleto(), request.aprovadoPor());
        return paraItemNovo(salvo);
    }

    public ItemGovernancaResponse reutilizarNovo(String id, GovernancaReutilizarRequest request) {
        CodigoCompleto model = exigirPendenteNovo(id);
        String escolhidoId = request.escolhidoId() == null ? "" : request.escolhidoId().trim();
        RegistroSimilar escolhido = (model.getSimilaresRegistrados() == null
                ? List.<RegistroSimilar>of() : model.getSimilaresRegistrados())
                .stream()
                .filter(s -> escolhidoId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("O item " + escolhidoId
                        + " não está entre os similares registrados deste pedido — "
                        + "só é possível reutilizar um dos matches apresentados ao solicitante."));
        String tipo = request.escolhidoTipo() == null || request.escolhidoTipo().isBlank()
                ? escolhido.getTipo()
                : request.escolhidoTipo().trim().toUpperCase(java.util.Locale.ROOT);

        catalogoService.desativarPendentesNaoUsados(model);

        String substituido = switch (tipo) {
            case "GS" -> {
                String antes = model.getGsId();
                model.setGsId(escolhido.getId());
                yield antes;
            }
            case "CF" -> {
                String antes = model.getCfId();
                model.setCfId(escolhido.getId());
                yield antes;
            }
            case "UM" -> {
                String antes = model.getUmId();
                model.setUmId(escolhido.getId());
                yield antes;
            }
            default -> {
                String antes = model.getCsId();
                model.setCsId(escolhido.getId());
                yield antes;
            }
        };
        PreviewService.ComponentesResolvidos resolvidos = previewService.resolver(PreviewRequest.deComposicao(
                model.getGsId(), model.getCfId(), model.getCsId(), model.getUmId()), false);
        model.setIdCompleto(PreviewService.montarIdCompleto(resolvidos));
        model.setDescricaoSnapshot(previewService.montarSnapshot(resolvidos));

        String motivo = request.observacao() == null ? "" : request.observacao().trim();
        String observacao = "Reutilização decidida na Governança — código " + escolhido.getId()
                + " (" + escolhido.getDescricao() + ") no lugar de " + substituido
                + (motivo.isEmpty() ? "" : " — " + motivo);
        model.setStatus(CodigoCompleto.STATUS_ATIVO);
        catalogoService.definirStatusComponentes(model, StatusComponente.ATIVO);
        model.setAprovadoPor(request.aprovadoPor());
        model.setObservacaoAprovacao(observacao);
        model.setAprovadoEm(Instant.now());
        CodigoCompleto salvo = codigoCompletoRepository.save(model);

        auditoriaService.registrar(EventoAuditoria.ACAO_REUTILIZACAO, "CODIGO_COMPLETO",
                salvo.getIdCompleto(), salvo.getDecisaoSimilaridade(), observacao,
                java.util.List.of(escolhido.getId()), java.util.List.of(escolhido.getScore()),
                request.aprovadoPor());
        log.info("[GOVERNANCA] Reutilizado: idCompleto={} -> {} ({}) por {}",
                salvo.getIdCompleto(), escolhido.getId(), tipo, request.aprovadoPor());
        return paraItemNovo(salvo);
    }

    public Map<String, Object> estatisticas() {
        Map<String, Object> dados = new java.util.LinkedHashMap<>();
        dados.put("catalogo", Map.of(
                "grupos", mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), GrupoServico.class),
                "fornecedores", mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), CodigoFornecedor.class),
                "servicos", mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), CodigoServico.class),
                "unidades", mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), UnidadeMedida.class),
                "codigosCompletos", codigoCompletoRepository.count()));
        dados.put("matriz", Map.of(
                "total", matrizDeParaRepository.count(),
                "duplicidade", matrizDeParaRepository.countByDecision("DUPLICIDADE"),
                "alerta", matrizDeParaRepository.countByDecision("ALERTA"),
                "original", matrizDeParaRepository.countByDecision("ORIGINAL"),
                "pendentes", matrizDeParaRepository.countByStatusRevisao(MatrizDePara.STATUS_PENDENTE),
                "revisados", matrizDeParaRepository.countByStatusRevisao(MatrizDePara.STATUS_REVISADO),
                "semPendencia", matrizDeParaRepository.countByStatusRevisao(MatrizDePara.STATUS_SEM_PENDENCIA)));
        dados.put("grupos", contagemPorGrupo());
        dados.put("porNatureza", agregacao("natureza", 8));
        dados.put("fornecedoresTop", agregacao("legacyData.codigoFornecedor", 8));
        dados.put("servicosVinculados", servicosMaisVinculados());
        dados.put("vinculos", Map.of(
                "ativos", codigoCompletoRepository.countByStatus(CodigoCompleto.STATUS_ATIVO),
                "pendentes", codigoCompletoRepository.countByStatus(CodigoCompleto.STATUS_PENDENTE),
                "rejeitados", codigoCompletoRepository.countByStatus(CodigoCompleto.STATUS_REJEITADO)));
        dados.put("unidades", agregacao("unidadeMedida", 12));
        dados.put("servicosTop", servicosMaisConsolidados());
        dados.put("tamanhos", distribuicaoTamanhos());
        dados.put("auditoria", Map.of(
                "eventos", eventoAuditoriaRepository.count(),
                "criacoes", eventoAuditoriaRepository.countByAction(EventoAuditoria.ACAO_CRIACAO_CODIGO_COMPLETO),
                "bloqueios", eventoAuditoriaRepository.countByAction(EventoAuditoria.ACAO_BLOQUEIO),
                "excecoes", excecaoRepository.count()));
        return dados;
    }

    private List<Map<String, Object>> agregacao(String campo, int limite) {
        var agg = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.group(campo)
                        .count().as("quantidade"),
                org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "quantidade")),
                org.springframework.data.mongodb.core.aggregation.Aggregation.limit(limite));
        List<Map<String, Object>> saida = new ArrayList<>();
        for (Map<String, Object> linha : mongoTemplate
                .aggregate(agg, MatrizDePara.class, Map.class).getMappedResults()) {
            Object nome = linha.get("_id");
            saida.add(Map.of("nome", nome == null ? "n/d" : String.valueOf(nome),
                    "quantidade", ((Number) linha.get("quantidade")).longValue()));
        }
        return saida;
    }

    private List<Map<String, Object>> servicosMaisVinculados() {
        var agg = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation
                        .group("csId").count().as("quantidade"),
                org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "quantidade")),
                org.springframework.data.mongodb.core.aggregation.Aggregation.limit(6));
        List<Map<String, Object>> saida = new ArrayList<>();
        for (Map<String, Object> linha : mongoTemplate
                .aggregate(agg, CodigoCompleto.class, Map.class).getMappedResults()) {
            String id = String.valueOf(linha.get("_id"));
            CodigoServico cs = mongoTemplate.findById(id, CodigoServico.class);
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", id);
            item.put("nome", cs != null && cs.getDescricao() != null ? cs.getDescricao() : id);
            item.put("grupo", cs != null && cs.getNatureza() != null ? cs.getNatureza() : "");
            item.put("quantidade", ((Number) linha.get("quantidade")).longValue());
            saida.add(item);
        }
        return saida;
    }

    private List<Map<String, Object>> servicosMaisConsolidados() {
        var agg = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.group("newId")
                        .count().as("quantidade")
                        .first("descricaoCanonica").as("nome")
                        .first("grupoServico").as("grupo"),
                org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "quantidade")),
                org.springframework.data.mongodb.core.aggregation.Aggregation.limit(6));
        List<Map<String, Object>> saida = new ArrayList<>();
        for (Map<String, Object> linha : mongoTemplate
                .aggregate(agg, MatrizDePara.class, Map.class).getMappedResults()) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("nome", linha.get("nome") == null ? "" : String.valueOf(linha.get("nome")));
            item.put("id", linha.get("_id") == null ? "" : String.valueOf(linha.get("_id")));
            item.put("grupo", linha.get("grupo") == null ? "" : String.valueOf(linha.get("grupo")));
            item.put("quantidade", ((Number) linha.get("quantidade")).longValue());
            saida.add(item);
        }
        return saida;
    }

    private List<Map<String, Object>> distribuicaoTamanhos() {
        List<Map<String, Object>> saida = new ArrayList<>();
        long ate19 = matrizDeParaRepository.countByLegacyDataQuantidadeCaracteresLessThan(20);
        long vinte = matrizDeParaRepository.countByLegacyDataQuantidadeCaracteresBetween(20, 29);
        long trinta = matrizDeParaRepository.countByLegacyDataQuantidadeCaracteresBetween(30, 39);
        long quarenta = matrizDeParaRepository.countByLegacyDataQuantidadeCaracteresGreaterThanEqual(40);
        saida.add(Map.of("nome", "até 19 caracteres", "quantidade", ate19));
        saida.add(Map.of("nome", "20 a 29", "quantidade", vinte));
        saida.add(Map.of("nome", "30 a 39", "quantidade", trinta));
        saida.add(Map.of("nome", "40+ (limite SAP)", "quantidade", quarenta));
        return saida;
    }

    public ItemGovernancaResponse reverterRevisao(String id, RevisaoMatrizRequest request) {
        MatrizDePara doc = matrizDeParaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro da matriz", id));
        if (MatrizDePara.STATUS_PENDENTE.equals(doc.getStatusRevisao())) {
            throw new BusinessException("O registro " + doc.getNewId() + " já está pendente de revisão.");
        }
        doc.setStatusRevisao(MatrizDePara.STATUS_PENDENTE);
        doc.setObservacaoRevisao("Decisão revertida por " + request.revisadoPor()
                + (request.observacao() == null ? "" : " — " + request.observacao()));
        MatrizDePara salvo = matrizDeParaRepository.save(doc);
        auditoriaService.registrar(EventoAuditoria.ACAO_REVISAO_HISTORICA, "MATRIZ_DE_PARA",
                salvo.getNewId(), salvo.getDecision(), "Decisão revertida — voltou para a fila",
                java.util.List.of(salvo.getNewId()),
                java.util.List.of(salvo.getScore() == null ? 0.0 : salvo.getScore()),
                request.revisadoPor());
        log.info("[GOVERNANCA] Decisão revertida: newId={} por {}", salvo.getNewId(), request.revisadoPor());
        return paraItemHistorico(salvo);
    }

    public long contarFila(String origem, String status, String decision, String query) {

        String statusMatriz = (status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status))
                ? MatrizDePara.STATUS_PENDENTE : status.trim().toUpperCase();
        String decisao = (decision == null || decision.isBlank() || "TODOS".equalsIgnoreCase(decision))
                ? null : decision.trim().toUpperCase();

        var criterios = new org.springframework.data.mongodb.core.query.Criteria();
        criterios = criterios.and("statusRevisao").is(statusMatriz);
        if (decisao != null) {
            criterios = criterios.and("decision").is(decisao);
        }
        if (query != null && !query.isBlank()) {
            String expr = TextoUtil.padraoBusca(query);
            criterios = criterios.andOperator(new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                    org.springframework.data.mongodb.core.query.Criteria.where("newId").regex(expr),
                    org.springframework.data.mongodb.core.query.Criteria.where("descricaoCanonica").regex(expr),
                    org.springframework.data.mongodb.core.query.Criteria.where("legacyData.legacyId").regex(expr)));
        }

        long totalHistorico = mongoTemplate.count(
                new org.springframework.data.mongodb.core.query.Query(criterios), MatrizDePara.class);

        boolean incluiNovos = (origem == null || origem.isBlank()
                || "TODOS".equalsIgnoreCase(origem) || "NOVOS".equalsIgnoreCase(origem))
                && decisao == null;
        long totalNovos = incluiNovos && (statusMatriz == null || MatrizDePara.STATUS_PENDENTE.equals(statusMatriz))
                ? codigoCompletoRepository.findByStatusOrderByCreatedAtDesc(CodigoCompleto.STATUS_PENDENTE).size()
                : 0;
        return totalHistorico + totalNovos;
    }

    public Map<String, Object> fechamentos(String de, String ate) {
        Instant inicio = parseData(de);
        Instant fim = parseData(ate);

        var criterios = new org.springframework.data.mongodb.core.query.Criteria("action").in(
                EventoAuditoria.ACAO_REVISAO_HISTORICA,
                EventoAuditoria.ACAO_APROVACAO,
                EventoAuditoria.ACAO_REJEICAO);
        if (inicio != null) {
            criterios = criterios.and("occurredAt").gte(inicio);
        }
        if (fim != null) {
            criterios = criterios.and("occurredAt").lte(fim);
        }

        var aggMensal = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(criterios),
                org.springframework.data.mongodb.core.aggregation.Aggregation
                        .project("occurredAt")
                        .and(org.springframework.data.mongodb.core.aggregation.DateOperators
                                .DateToString.dateOf("occurredAt").toString("%Y-%m")).as("mes"),
                org.springframework.data.mongodb.core.aggregation.Aggregation.group("mes").count().as("quantidade"),
                org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.ASC, "_id")));
        List<Map<String, Object>> porMes = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> linha : mongoTemplate
                .aggregate(aggMensal, EventoAuditoria.class, Map.class).getMappedResults()) {
            long qtd = ((Number) linha.get("quantidade")).longValue();
            total += qtd;
            porMes.add(Map.of("mes", String.valueOf(linha.get("_id")), "quantidade", qtd));
        }

        var criteriosServico = org.springframework.data.mongodb.core.query.Criteria
                .where("action").is(EventoAuditoria.ACAO_REVISAO_HISTORICA);
        if (inicio != null) {
            criteriosServico = criteriosServico.and("occurredAt").gte(inicio);
        }
        if (fim != null) {
            criteriosServico = criteriosServico.and("occurredAt").lte(fim);
        }
        var aggTop = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(criteriosServico),
                org.springframework.data.mongodb.core.aggregation.Aggregation
                        .group("entityId").count().as("quantidade"),
                org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "quantidade")),
                org.springframework.data.mongodb.core.aggregation.Aggregation.limit(6));
        List<Map<String, Object>> top = new ArrayList<>();
        for (Map<String, Object> linha : mongoTemplate
                .aggregate(aggTop, EventoAuditoria.class, Map.class).getMappedResults()) {
            String id = String.valueOf(linha.get("_id"));
            CodigoServico servico = mongoTemplate.findById(id, CodigoServico.class);
            String nome = servico != null && servico.getDescricao() != null ? servico.getDescricao() : id;
            top.add(Map.of("id", id, "nome", nome,
                    "quantidade", ((Number) linha.get("quantidade")).longValue()));
        }

        Map<String, Object> resposta = new java.util.LinkedHashMap<>();
        resposta.put("total", total);
        resposta.put("porMes", porMes);
        resposta.put("servicosTop", top);
        resposta.put("de", de == null ? "" : de);
        resposta.put("ate", ate == null ? "" : ate);
        return resposta;
    }

    private static Instant parseData(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(valor.contains("T") ? valor : valor + "T00:00:00Z");
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> listarEventos(String acao) {
        List<EventoAuditoria> eventos = (acao == null || acao.isBlank() || "TODOS".equalsIgnoreCase(acao))
                ? eventoAuditoriaRepository.findTop100ByOrderByOccurredAtDesc()
                : eventoAuditoriaRepository.findByActionOrderByOccurredAtDesc(acao.trim().toUpperCase());
        return eventos.stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("actorId", e.getActorId());
                    m.put("action", e.getAction());
                    m.put("entityType", e.getEntityType());
                    m.put("entityId", e.getEntityId());
                    m.put("occurredAt", e.getOccurredAt());
                    m.put("decision", e.getDecision());
                    m.put("reason", e.getReason());
                    m.put("candidateIds", e.getCandidateIds());
                    m.put("similarityScores", e.getSimilarityScores());
                    m.put("correlationId", e.getCorrelationId());
                    return m;
                })
                .toList();
    }

    public List<ExcecaoResponse> listarExcecoes() {
        return excecaoRepository.findTop50ByOrderByDataHoraDesc().stream()
                .map(e -> new ExcecaoResponse(e.getId(), e.getSolicitante(), e.getDataHora(),
                        e.getPayloadOriginal(), e.getSimilaresEncontrados(), e.getJustificativa(),
                        e.getDecisaoTomada(), e.getObservacao(), e.getCodigoCompletoId()))
                .toList();
    }

    private CodigoCompleto exigirPendenteNovo(String id) {
        CodigoCompleto model = codigoCompletoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código Completo", id));
        if (!CodigoCompleto.STATUS_PENDENTE.equals(model.getStatus())) {
            throw new BusinessException("O item " + model.getIdCompleto()
                    + " não está pendente de validação (status atual: " + model.getStatus() + ").");
        }
        return model;
    }

    private ItemGovernancaResponse paraItemHistorico(MatrizDePara m) {
        MatrizDePara.LegacyData l = m.getLegacyData();
        String observacao = m.getObservacaoRevisao();
        boolean aceitouRepeticao = MatrizDePara.STATUS_REVISADO.equals(m.getStatusRevisao())
                && ("DUPLICIDADE".equals(m.getDecision()) || "ALERTA".equals(m.getDecision()))
                && observacao != null && observacao.trim().startsWith("MANTER");
        String motivoExcecao = observacao == null
                ? null : observacao.replaceFirst("^MANTER\\s*[—-]\\s*", "").trim();
        return new ItemGovernancaResponse(
                "HISTORICO",
                m.getId(),
                m.getNewId(),
                m.getDescricaoCanonica(),
                m.getGrupoServico(),
                l == null ? null : String.valueOf(l.getCodigoFornecedor()),
                m.getUnidadeMedida(),
                l == null ? null : l.getLegacyId(),
                l == null ? null : l.getLegacyDescription(),
                m.getDecision(),
                m.getScore() == null ? 0.0 : m.getScore(),
                m.getReason(),
                m.getReviewedBy(),
                m.getObservacaoRevisao(),
                m.getStatusRevisao(),
                m.getNatureza(),
                List.of(),
                aceitouRepeticao,
                aceitouRepeticao ? motivoExcecao : null);
    }

    private ItemGovernancaResponse paraItemNovo(CodigoCompleto c) {
        List<CandidatoSimilarResponse> similares = (c.getSimilaresRegistrados() == null
                ? List.<RegistroSimilar>of() : c.getSimilaresRegistrados())
                .stream()
                .map(s -> new CandidatoSimilarResponse(s.getPosicao(), s.getId(), s.getCodigoOriginal(), s.getDescricao(),
                        s.getScore(), 0.0, 0.0, List.of(), s.getMotivo(), s.isCasoIsolado(), s.getMotivoCasoIsolado(),
                        s.getTipo()))
                .toList();
        CodigoServico csNovo = mongoTemplate.findById(c.getCsId(), CodigoServico.class);
        boolean casoIsolado = c.getDecisaoSimilaridade() != null && !"PERMITIR".equals(c.getDecisaoSimilaridade());
        String motivoCasoIsolado = c.getObservacaoAprovacao() != null && !c.getObservacaoAprovacao().isBlank()
                ? c.getObservacaoAprovacao() : c.getJustificativa();
        return new ItemGovernancaResponse(
                "NOVO_CADASTRO",
                c.getId(),
                c.getIdCompleto(),
                c.getDescricaoSnapshot(),
                c.getGsId(),
                c.getCfId(),
                c.getUmId(),
                c.getCsId(),
                null,
                c.getDecisaoSimilaridade(),
                c.getMelhorScore() == null ? 0.0 : c.getMelhorScore(),
                c.getJustificativa(),
                c.getUsuarioCriacaoId(),
                c.getJustificativa(),
                c.getStatus(),
                csNovo != null ? csNovo.getNatureza() : null,
                similares,
                casoIsolado,
                motivoCasoIsolado);
    }
}
