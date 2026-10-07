package br.com.gerdau.servicos.api.maintenance;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.text.TextNormalizer;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Preenche campos NORMALIZADOS que faltem em documentos já existentes (legado importado sem
 * nomeNormalizado / descricaoNormalizada / quantidadeCaracteres). Só ADICIONA campos ausentes;
 * nunca altera nome/descrição originais. Ligue uma vez com BACKFILL_NORMALIZED=true.
 */
@Component
@Order(5)
@ConditionalOnProperty(name = "app.maintenance.backfill-normalized", havingValue = "true")
public class BackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BackfillRunner.class);

    private final MongoTemplate mongo;
    private final TextNormalizer normalizer;

    public BackfillRunner(MongoTemplate mongo, TextNormalizer normalizer) {
        this.mongo = mongo;
        this.normalizer = normalizer;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (TipoComponente t : TipoComponente.values()) {
            Query missing = new Query(new Criteria().orOperator(
                    Criteria.where(t.normalizedField()).exists(false),
                    Criteria.where(t.normalizedField()).is(null),
                    Criteria.where(t.normalizedField()).is("")));
            List<Document> docs = mongo.find(missing, Document.class, t.collection());
            int updated = 0;
            for (Document d : docs) {
                String text = Objects.toString(d.get(t.textField()), "");
                if (text.isBlank()) continue;
                Update update = new Update().set(t.normalizedField(), normalizer.normalize(text));
                if (t == TipoComponente.CS && d.get("quantidadeCaracteres") == null) {
                    update.set("quantidadeCaracteres", text.codePointCount(0, text.length()));
                }
                mongo.updateFirst(Query.query(Criteria.where("_id").is(d.get("_id"))), update, t.collection());
                updated++;
            }
            log.info("Backfill {}: {} documento(s) sem normalizado, {} atualizado(s).", t.collection(), docs.size(), updated);
        }
    }
}
