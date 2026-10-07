package br.com.gerdau.servicos.api.persistence;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CodigoCompletoRepository {

    public static final String COLLECTION = "codigos_completos";

    private final MongoTemplate mongo;

    public CodigoCompletoRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<Document> findById(String idCompleto) {
        return Optional.ofNullable(mongo.findById(idCompleto, Document.class, COLLECTION));
    }

    public boolean existsByComposicao(String gsId, String cfId, String csId, String umId) {
        Query query = Query.query(Criteria.where("gsId").is(gsId).and("cfId").is(cfId)
                .and("csId").is(csId).and("umId").is(umId));
        return mongo.exists(query, COLLECTION);
    }

    public boolean existsById(String idCompleto) {
        return mongo.exists(Query.query(Criteria.where("_id").is(idCompleto)), COLLECTION);
    }

    /** Lança DuplicateKeyException se o código completo já existir. */
    public void insert(Document document) {
        mongo.insert(document, COLLECTION);
    }
}
