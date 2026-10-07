package br.com.gerdau.servicos.api.search;

import br.com.gerdau.servicos.api.config.AppProperties;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Atlas Search ($search). Exige o índice "default" (ou o nome em ATLAS_SEARCH_INDEX) em cada
 * coleção de componentes — crie com: mongosh "$MONGODB_URI" scripts/mongo/create-atlas-search-indexes.js
 * Os índices ficam sobre o campo NORMALIZADO (nomeNormalizado / descricaoNormalizada).
 */
@Component
@ConditionalOnProperty(name = "app.search.engine", havingValue = "atlas")
public class AtlasSearchClient implements SearchClient {

    private final MongoTemplate mongo;
    private final String indexName;

    public AtlasSearchClient(MongoTemplate mongo, AppProperties props) {
        this.mongo = mongo;
        this.indexName = props.search().atlasIndexName();
    }

    @Override
    public List<SearchHit> autocomplete(TipoComponente tipo, List<String> prefixTokens, int limit) {
        if (prefixTokens.isEmpty()) return List.of();
        return run(tipo, String.join(" ", prefixTokens), limit, 1);
    }

    @Override
    public List<SearchHit> candidates(TipoComponente tipo, List<String> contentTokens, int limit) {
        if (contentTokens.isEmpty()) return List.of();
        return run(tipo, String.join(" ", contentTokens), limit, 2);
    }

    private List<SearchHit> run(TipoComponente tipo, String query, int limit, int maxEdits) {
        String path = tipo.normalizedField();
        Document fuzzy = new Document("maxEdits", maxEdits).append("prefixLength", 1);

        Document search = new Document("index", indexName)
                .append("compound", new Document("should", List.of(
                        new Document("autocomplete", new Document("query", query).append("path", path).append("fuzzy", fuzzy)),
                        new Document("text", new Document("query", query).append("path", path).append("fuzzy", fuzzy))))
                        .append("minimumShouldMatch", 1));

        List<Document> pipeline = List.of(
                new Document("$search", search),
                new Document("$limit", limit),
                new Document("$addFields", new Document("_searchScore", new Document("$meta", "searchScore"))));

        List<SearchHit> hits = new ArrayList<>();
        for (Document doc : mongo.getCollection(tipo.collection()).aggregate(pipeline)) {
            Number score = (Number) doc.remove("_searchScore");
            hits.add(new SearchHit(String.valueOf(doc.get("_id")), doc, score == null ? 0.0 : score.doubleValue()));
        }
        return hits;
    }
}
