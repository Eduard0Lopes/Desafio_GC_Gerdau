package com.gerdau.desafio.seed;

import com.gerdau.desafio.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Complementação idempotente do schema (especificação, seção 5): garante
 * {@code active}, {@code createdAt} e {@code updatedAt} nos documentos —
 * inclusive nos migrados do Time de IA — sem reprocessar o que já está completo.
 */
@Component
@Order(10)
public class SchemaBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaBackfill.class);

    private final MongoTemplate mongoTemplate;

    public SchemaBackfill(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            long ajustados = 0;
            ajustados += completar(GrupoServico.class);
            ajustados += completar(CodigoFornecedor.class);
            ajustados += completar(CodigoServico.class);
            ajustados += completar(UnidadeMedida.class);
            ajustados += classificarNaturezas();
            if (ajustados > 0) {
                log.info("[SCHEMA] Backfill executado em {} documento(s) (campos base + natureza).", ajustados);
            }
        } catch (Exception e) {
            log.warn("[SCHEMA] Backfill não executado agora ({}).", e.getMessage());
        }
    }

    /** Classifica a natureza dos documentos que ainda não possuem (dados migrados antigos). */
    private long classificarNaturezas() {
        long ajustados = 0;
        for (MatrizDePara doc : mongoTemplate.find(
                new Query(Criteria.where("natureza").is(null)), MatrizDePara.class)) {
            doc.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(doc.getDescricaoCanonica()));
            mongoTemplate.save(doc);
            ajustados++;
        }
        for (CodigoServico cs : mongoTemplate.find(
                new Query(Criteria.where("natureza").is(null)), CodigoServico.class)) {
            cs.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(cs.getDescricao()));
            mongoTemplate.save(cs);
            ajustados++;
        }
        return ajustados;
    }

    private long completar(Class<?> entidade) {
        Query semCampos = new Query(new Criteria().andOperator(
                new Criteria().orOperator(
                        Criteria.where("active").exists(false),
                        Criteria.where("active").is(null)),
                new Criteria().orOperator(
                        Criteria.where("createdAt").exists(false),
                        Criteria.where("createdAt").is(null))));
        Update atualizacao = new Update()
                .set("active", true)
                .set("createdAt", Instant.now())
                .set("updatedAt", Instant.now());
        return mongoTemplate.updateMulti(semCampos, atualizacao, entidade).getModifiedCount();
    }
}
