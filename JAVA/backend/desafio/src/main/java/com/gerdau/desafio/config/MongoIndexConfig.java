package com.gerdau.desafio.config;

import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.CodigoServico;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void criarIndices() {
        log.info("[MONGO] Criando índices...");

        IndexOperations idxCodigoCompleto = mongoTemplate.indexOps(CodigoCompleto.class);
        idxCodigoCompleto.createIndex(new Index().on("gsId", Sort.Direction.ASC));
        idxCodigoCompleto.createIndex(new Index().on("cfId", Sort.Direction.ASC));
        idxCodigoCompleto.createIndex(new Index().on("csId", Sort.Direction.ASC));
        idxCodigoCompleto.createIndex(new Index().on("origem", Sort.Direction.ASC));

        IndexOperations idxCodigoServico = mongoTemplate.indexOps(CodigoServico.class);
        idxCodigoServico.createIndex(new Index().on("descricaoNormalizada", Sort.Direction.ASC));
        idxCodigoServico.createIndex(new Index().on("codigoOriginal", Sort.Direction.ASC));
        idxCodigoServico.createIndex(new Index().on("origem", Sort.Direction.ASC));

        log.info("[MONGO] Índices criados com sucesso.");
    }
}