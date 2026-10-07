package br.com.gerdau.servicos.api.maintenance;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.persistence.CodigoCompletoRepository;
import com.mongodb.MongoException;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cria (se ainda não existirem) os índices que a aplicação espera (doc. 14.3). Nunca derruba a aplicação:
 * se o usuário do banco não puder criar índices, ou se já existir um índice conflitante, só avisa no log.
 * Desligue com ENSURE_INDEXES=false se o seu DBA gerencia os índices.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.maintenance.ensure-indexes", havingValue = "true", matchIfMissing = true)
public class MongoIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongo;

    public MongoIndexInitializer(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (TipoComponente t : TipoComponente.values()) {
            create(t.collection(), Indexes.ascending(t.normalizedField()),
                    new IndexOptions().name("idx_" + t.normalizedField()));
        }
        String cc = CodigoCompletoRepository.COLLECTION;
        create(cc, Indexes.ascending("idCompleto"), new IndexOptions().name("uk_idCompleto").unique(true));
        create(cc, Indexes.ascending("gsId", "cfId", "csId", "umId"),
                new IndexOptions().name("uk_composicao").unique(true));
        for (String field : List.of("gsId", "cfId", "csId", "umId")) {
            create(cc, Indexes.ascending(field), new IndexOptions().name("idx_" + field));
        }
        create("idempotency_keys", Indexes.ascending("createdAt"),
                new IndexOptions().name("ttl_createdAt").expireAfter(24L, TimeUnit.HOURS));
        create("solicitacoes_governanca", Indexes.ascending("status", "createdAt"),
                new IndexOptions().name("idx_status_createdAt"));
    }

    private void create(String collection, Bson keys, IndexOptions options) {
        try {
            mongo.getCollection(collection).createIndex(keys, options);
            log.info("Índice garantido: {}.{}", collection, options.getName());
        } catch (MongoException e) {
            log.warn("Não foi possível criar o índice {}.{}: {} — a aplicação continua; veja docs/CONECTAR-MONGODB.md",
                    collection, options.getName(), e.getMessage());
        }
    }
}
