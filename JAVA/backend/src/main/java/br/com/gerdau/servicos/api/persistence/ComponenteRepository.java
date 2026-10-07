package br.com.gerdau.servicos.api.persistence;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Acesso às quatro coleções de componentes. Usa Document (e não classes fixas) de propósito:
 * o legado importado pode ter campos faltando ou com tipos diferentes e a leitura não pode quebrar.
 */
@Repository
public class ComponenteRepository {

    private final MongoTemplate mongo;

    public ComponenteRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Optional<Document> findById(TipoComponente tipo, String id) {
        return Optional.ofNullable(mongo.findById(id, Document.class, tipo.collection()));
    }

    public List<Document> findByIds(TipoComponente tipo, Collection<String> ids) {
        return mongo.find(Query.query(Criteria.where("_id").in(ids)), Document.class, tipo.collection());
    }

    public List<Document> findByIdPrefix(TipoComponente tipo, String prefix, int limit) {
        Query query = new Query(Criteria.where("_id").regex("^" + Pattern.quote(prefix)))
                .with(Sort.by("_id")).limit(limit);
        return mongo.find(query, Document.class, tipo.collection());
    }

    public List<Document> findByNormalizedText(TipoComponente tipo, String normalized, int limit) {
        Query query = new Query(Criteria.where(tipo.normalizedField()).is(normalized)).limit(limit);
        return mongo.find(query, Document.class, tipo.collection());
    }

    public List<Document> findBySigla(String sigla) {
        Query query = new Query(Criteria.where("sigla").regex("^" + Pattern.quote(sigla) + "$", "i")).limit(5);
        return mongo.find(query, Document.class, TipoComponente.UM.collection());
    }

    public List<Document> listFirst(TipoComponente tipo, int limit) {
        Query query = new Query().with(Sort.by(tipo.normalizedField())).limit(limit);
        return mongo.find(query, Document.class, tipo.collection());
    }

    /** Lança DuplicateKeyException (_id repetido) — quem chama decide o que fazer. */
    public void insert(TipoComponente tipo, Document document) {
        mongo.insert(document, tipo.collection());
    }

    /** Maior _id puramente numérico da coleção (0 se não houver): semente da sequência de IDs. */
    public long maxNumericId(TipoComponente tipo) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document("_id", new Document("$regex", "^[0-9]+$"))),
                new Document("$group", new Document("_id", null)
                        .append("max", new Document("$max", new Document("$toLong", "$_id")))));
        Document result = mongo.getCollection(tipo.collection()).aggregate(pipeline).first();
        if (result == null || result.get("max") == null) {
            return 0L;
        }
        return ((Number) result.get("max")).longValue();
    }
}
