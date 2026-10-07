package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.persistence.ComponenteRepository;
import br.com.gerdau.servicos.api.text.IdFormatter;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Sequência controlada e atômica (doc. 8.2) por coleção. Na primeira vez é "semeada" com o maior ID
 * numérico já existente no banco, para nunca colidir com o que foi migrado do legado.
 */
@Service
public class SequenceService {

    private static final String COLLECTION = "sequences";

    private final MongoTemplate mongo;
    private final ComponenteRepository repo;
    private final IdFormatter ids;

    public SequenceService(MongoTemplate mongo, ComponenteRepository repo, IdFormatter ids) {
        this.mongo = mongo;
        this.repo = repo;
        this.ids = ids;
    }

    public String proximoId(TipoComponente tipo) {
        String key = tipo.collection();
        Query byKey = Query.query(Criteria.where("_id").is(key));
        if (mongo.findOne(byKey, Document.class, COLLECTION) == null) {
            long max = repo.maxNumericId(tipo);
            // setOnInsert é atômico e idempotente: se duas instâncias semearem juntas, só uma vale.
            mongo.upsert(byKey, new Update().setOnInsert("seq", max), COLLECTION);
        }
        Document updated = mongo.findAndModify(byKey, new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true), Document.class, COLLECTION);
        long next = ((Number) updated.get("seq")).longValue();
        return ids.format(tipo, next);
    }
}
