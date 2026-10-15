package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.GovernancaDecisaoRequest;
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
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Esteira de governança (seção 12):
 *  - REVISÃO HISTÓRICA: registros da matriz com decisão DUPLICIDADE/ALERTA,
 *    pré-carregados com os scores reais como evidência (reviewedBy/reviewedAt — seção 9.6);
 *  - NOVOS CADASTROS: itens enviados para validação humana pela aplicação.
 */
@Service
public class GovernancaService {

    private static final Logger log = LoggerFactory.getLogger(GovernancaService.class);

    private final MatrizDeParaRepository matrizDeParaRepository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final ExcecaoRepository excecaoRepository;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final AuditoriaService auditoriaService;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public GovernancaService(MatrizDeParaRepository matrizDeParaRepository,
                             CodigoCompletoRepository codigoCompletoRepository,
                             ExcecaoRepository excecaoRepository,
                             EventoAuditoriaRepository eventoAuditoriaRepository,
                             AuditoriaService auditoriaService,
                             org.springframework.data.mongodb.core.MongoTemplate mongoTemplate) {
        this.matrizDeParaRepository = matrizDeParaRepository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.excecaoRepository = excecaoRepository;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
        this.auditoriaService = auditoriaService;
        this.mongoTemplate = mongoTemplate;
    }

    /** Fila unificada: histórico (matriz) + novos cadastros, com filtro de status/decisão e paginacao. */
    public List<ItemGovernancaResponse> listarPendentes(String origem, String query, int limite,
                                                       String status, String decision, int pagina) {
        List<ItemGovernancaResponse> itens = new ArrayList<>();
        boolean incluiHistorico = origem == null || origem.isBlank()
                || "TODOS".equalsIgnoreCase(origem) || "HISTORICO".equalsIgnoreCase(origem);
        // visões por decisão (DUPLICIDADE/ALERTA) são do histórico — novos cadastros não entram
        String decisao = (decision == null || decision.isBlank() || "TODOS".equalsIgnoreCase(decision))
                ? null : decision.trim().toUpperCase();
        boolean incluiNovos = (origem == null || origem.isBlank()
                || "TODOS".equalsIgnoreCase(origem) || "NOVOS".equalsIgnoreCase(origem))
                && decisao == null;

        // null = sem filtro de status (visões "TODOS" — ex.: todos os quase repetidos)
        String statusMatriz = (status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status))
                ? null : status.trim().toUpperCase();
        Pageable paginacao = PageRequest.of(Math.max(pagina, 0), limite);

        if (incluiHistorico) {
            List<MatrizDePara> matriz;
            if (query != null && !query.isBlank()) {
                matriz = matrizDeParaRepository.buscarPorTexto(regex(query), PageRequest.of(0, limite)).stream()
                        .filter(m -> statusMatriz == null || statusMatriz.equals(m.getStatusRevisao()))
                        .filter(m -> decisao == null || decisao.equals(m.getDecision()))
                        .limit(limite).toList();
            } else if (decisao != null && statusMatriz != null) {
                matriz = matrizDeParaRepository.findByDecisionAndStatusRevisaoOrderByScoreDesc(
                        decisao, statusMatriz, paginacao);
            } else if (decisao != null) {
                matriz = matrizDeParaRepository.findByDecisionOrderByScoreDesc(decisao, paginacao);
            } else if (statusMatriz != null) {
                matriz = matrizDeParaRepository.findByStatusRevisaoOrderByScoreDesc(statusMatriz, paginacao);
            } else {
                matriz = matrizDeParaRepository.findAll(paginacao).getContent();
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

    /** Uso por Grupo de Serviço — base para os gráficos de perfil da base. */
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

    /**
     * Revisão humana do histórico (seção 9.6): reviewedBy/reviewedAt + motivo.
     * A decisão pode ser refeita (o histórico de decisões fica na auditoria).
     */
    public ItemGovernancaResponse revisarHistorico(String id, RevisaoMatrizRequest request) {
        MatrizDePara doc = matrizDeParaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro da matriz", id));
        String motivo = request.observacao() == null ? "" : request.observacao().trim();
        if (motivo.isBlank()) {
            throw new BusinessException("Informe o motivo da decisão — ele é obrigatório.");
        }
        doc.setStatusRevisao(MatrizDePara.STATUS_REVISADO);
        doc.setReviewedBy(request.revisadoPor());
        doc.setReviewedAt(Instant.now());
        doc.setObservacaoRevisao((request.acao() == null ? "" : request.acao() + " — ") + motivo);
        MatrizDePara salvo = matrizDeParaRepository.save(doc);
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
        model.setStatus(CodigoCompleto.STATUS_ATIVO);
        model.setAprovadoPor(request.aprovadoPor());
        model.setObservacaoAprovacao(request.observacao());
        model.setAprovadoEm(Instant.now());
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
        model.setAprovadoPor(request.aprovadoPor());
        model.setObservacaoAprovacao(request.observacao());
        model.setAprovadoEm(Instant.now());
        CodigoCompleto salvo = codigoCompletoRepository.save(model);
        auditoriaService.registrar(EventoAuditoria.ACAO_REJEICAO, "CODIGO_COMPLETO",
                salvo.getIdCompleto(), salvo.getDecisaoSimilaridade(), request.observacao(),
                java.util.List.of(), java.util.List.of(), request.aprovadoPor());
        log.info("[GOVERNANCA] Rejeitado: idCompleto={} por {}", salvo.getIdCompleto(), request.aprovadoPor());
        return paraItemNovo(salvo);
    }

    /** Estatísticas detalhadas para o Painel Admin (gráficos e exportação). */
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

    /** Contagem agrupada por campo (gráfico de categorias). */
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

    /** Códigos de serviço mais vinculados (quantidade de vínculos em codigos_completos). */
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

    /** Códigos de serviço mais consolidados (quantos legados cada canônico absorveu). */
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

    /** Distribuição dos tamanhos de descrição (faixas do limite SAP). */
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

    /** Reverter a decisão humana: o item volta para a fila de pendentes (auditoria preservada). */
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

    /** Contagem total da fila (para a paginação informar "Página X de Y"). */
    public long contarFila(String origem, String status, String decision, String query) {
        // null = sem filtro de status (visões "TODOS" — ex.: todos os quase repetidos)
        String statusMatriz = (status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status))
                ? null : status.trim().toUpperCase();
        String decisao = (decision == null || decision.isBlank() || "TODOS".equalsIgnoreCase(decision))
                ? null : decision.trim().toUpperCase();

        var criterios = new org.springframework.data.mongodb.core.query.Criteria();
        if (statusMatriz != null) {
            criterios = criterios.and("statusRevisao").is(statusMatriz);
        }
        if (decisao != null) {
            criterios = criterios.and("decision").is(decisao);
        }
        if (query != null && !query.isBlank()) {
            String expr = regex(query);
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

    /**
     * Fechamentos (decisões humanas) por período — métrica mensal de aprovações,
     * consolidações e rejeições, com os serviços mais consolidados no intervalo.
     */
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

    /** Trilha de auditoria — opcionalmente filtrada por ação. */
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
                List.of());
    }

    private ItemGovernancaResponse paraItemNovo(CodigoCompleto c) {
        List<CandidatoSimilarResponse> similares = (c.getSimilaresRegistrados() == null
                ? List.<RegistroSimilar>of() : c.getSimilaresRegistrados())
                .stream()
                .map(s -> new CandidatoSimilarResponse(s.posicao(), s.id(), s.codigoOriginal(), s.descricao(),
                        s.score(), 0.0, 0.0, List.of(), s.motivo()))
                .toList();
        CodigoServico csNovo = mongoTemplate.findById(c.getCsId(), CodigoServico.class);
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
                similares);
    }

    private static String regex(String termo) {
        return "(?i)" + java.util.regex.Pattern.quote(termo.trim());
    }
}

