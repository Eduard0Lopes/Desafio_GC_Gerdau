package com.gerdau.desafio.config;

import com.gerdau.desafio.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;

/**
 * Índices do MongoDB — documento técnico, seção 14.3 + padrões de consulta reais.
 *
 * Executa uma única vez, após a aplicação estar pronta, e nunca derruba a
 * aplicação se o banco estiver indisponível (apenas registra aviso).
 * Pode ser desligado com {@code app.mongo.ensure-indexes=false}.
 */
@Component
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;
    private final boolean ensureIndexes;

    public MongoIndexConfig(MongoTemplate mongoTemplate,
                            @Value("${app.mongo.ensure-indexes:true}") boolean ensureIndexes) {
        this.mongoTemplate = mongoTemplate;
        this.ensureIndexes = ensureIndexes;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void criarIndices() {
        if (!ensureIndexes) {
            log.info("[MONGO] Verificação de índices desabilitada (app.mongo.ensure-indexes=false).");
            return;
        }
        try {
            log.info("[MONGO] Verificando índices (seção 14.3)...");

            // ---- codigos_completos: composição única + filtros + fila de governança
            IndexOperations idxCompletos = mongoTemplate.indexOps(CodigoCompleto.class);
            idxCompletos.createIndex(new Index().on("idCompleto", Sort.Direction.ASC).unique());
            idxCompletos.createIndex(new Index().on("gsId", Sort.Direction.ASC));
            idxCompletos.createIndex(new Index().on("cfId", Sort.Direction.ASC));
            idxCompletos.createIndex(new Index().on("csId", Sort.Direction.ASC));
            idxCompletos.createIndex(new Index().on("umId", Sort.Direction.ASC));
            idxCompletos.createIndex(new Index().on("status", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
            idxCompletos.createIndex(new Index().on("origem", Sort.Direction.ASC));
            idxCompletos.createIndex(new Index().on("idempotencyKey", Sort.Direction.ASC).unique().sparse());

            // ---- codigos_servico: busca textual + normalizada + legados
            IndexOperations idxServico = mongoTemplate.indexOps(CodigoServico.class);
            idxServico.createIndex(new Index().on("descricaoNormalizada", Sort.Direction.ASC));
            idxServico.createIndex(new Index().on("origem", Sort.Direction.ASC));
            idxServico.createIndex(new Index().on("active", Sort.Direction.ASC));
            idxServico.createIndex(new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("descricao")
                    .onField("descricaoNormalizada")
                    .withDefaultLanguage("portuguese")
                    .named("idx_text_servico")
                    .build());

            // ---- matriz_depara: fila de revisão (status+score), de-para e filtros
            IndexOperations idxMatriz = mongoTemplate.indexOps(MatrizDePara.class);
            idxMatriz.createIndex(new Index().on("statusRevisao", Sort.Direction.ASC).on("score", Sort.Direction.DESC));
            idxMatriz.createIndex(new Index().on("newId", Sort.Direction.ASC));
            idxMatriz.createIndex(new Index().on("legacyData.legacyId", Sort.Direction.ASC));
            idxMatriz.createIndex(new Index().on("decision", Sort.Direction.ASC));
            idxMatriz.createIndex(new Index().on("grupoServico", Sort.Direction.ASC));
            idxMatriz.createIndex(new Index().on("unidadeMedida", Sort.Direction.ASC));

            // ---- componentes: consultas dos seletores (?query=) e cadastro
            IndexOperations idxGrupo = mongoTemplate.indexOps(GrupoServico.class);
            idxGrupo.createIndex(new Index().on("nomeNormalizado", Sort.Direction.ASC));
            idxGrupo.createIndex(new Index().on("active", Sort.Direction.ASC));

            IndexOperations idxFornecedor = mongoTemplate.indexOps(CodigoFornecedor.class);
            idxFornecedor.createIndex(new Index().on("nomeNormalizado", Sort.Direction.ASC));
            idxFornecedor.createIndex(new Index().on("codigoFornecedorSAP", Sort.Direction.ASC));
            idxFornecedor.createIndex(new Index().on("active", Sort.Direction.ASC));

            IndexOperations idxUnidade = mongoTemplate.indexOps(UnidadeMedida.class);
            idxUnidade.createIndex(new Index().on("sigla", Sort.Direction.ASC).unique());
            idxUnidade.createIndex(new Index().on("nomeNormalizado", Sort.Direction.ASC));

            // ---- governança/auditoria: listagens por data e consulta por correlationId
            IndexOperations idxExcecoes = mongoTemplate.indexOps(Excecao.class);
            idxExcecoes.createIndex(new Index().on("dataHora", Sort.Direction.DESC));
            idxExcecoes.createIndex(new Index().on("codigoCompletoId", Sort.Direction.ASC));

            IndexOperations idxEventos = mongoTemplate.indexOps(EventoAuditoria.class);
            idxEventos.createIndex(new Index().on("occurredAt", Sort.Direction.DESC));
            idxEventos.createIndex(new Index().on("action", Sort.Direction.ASC).on("occurredAt", Sort.Direction.DESC));
            idxEventos.createIndex(new Index().on("correlationId", Sort.Direction.ASC));
            idxEventos.createIndex(new Index().on("entityId", Sort.Direction.ASC));

            log.info("[MONGO] Índices verificados com sucesso.");
        } catch (Exception e) {
            log.warn("[MONGO] Não foi possível verificar os índices agora ({}). A aplicação continua no ar.",
                    e.getMessage());
        }
    }
}
