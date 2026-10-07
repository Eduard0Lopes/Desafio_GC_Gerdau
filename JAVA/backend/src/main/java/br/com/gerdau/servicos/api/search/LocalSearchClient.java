package br.com.gerdau.servicos.api.search;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import org.bson.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Motor de busca "sem dependências": usa regex do próprio MongoDB (funciona em qualquer MongoDB,
 * inclusive o do docker compose). Bom para protótipo e volumes pequenos/médios; em produção, com
 * muitos itens, prefira app.search.engine=atlas (índices dedicados, fuzzy e stemming reais).
 * Os tokens vêm de TextNormalizer (apenas [a-z0-9]), portanto são seguros para montar a regex.
 */
@Component
@ConditionalOnProperty(name = "app.search.engine", havingValue = "local", matchIfMissing = true)
public class LocalSearchClient implements SearchClient {

    private final MongoTemplate mongo;

    public LocalSearchClient(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<SearchHit> autocomplete(TipoComponente tipo, List<String> prefixTokens, int limit) {
        if (prefixTokens.isEmpty()) return List.of();
        StringBuilder regex = new StringBuilder("^");
        for (String token : prefixTokens) {
            regex.append("(?=.*(?:^| )").append(token).append(')');
        }
        return find(tipo, regex.toString(), limit);
    }

    @Override
    public List<SearchHit> candidates(TipoComponente tipo, List<String> contentTokens, int limit) {
        List<String> prefixes = contentTokens.stream()
                .filter(t -> t.length() >= 3)
                .map(t -> t.substring(0, Math.min(4, t.length())))
                .distinct()
                .toList();
        if (prefixes.isEmpty()) return List.of();
        return find(tipo, "(?:^| )(?:" + String.join("|", prefixes) + ")", limit);
    }

    private List<SearchHit> find(TipoComponente tipo, String regex, int limit) {
        Query query = new Query(Criteria.where(tipo.normalizedField()).regex(regex)).limit(limit);
        return mongo.find(query, Document.class, tipo.collection()).stream()
                .map(d -> new SearchHit(String.valueOf(d.get("_id")), d, 0.0))
                .toList();
    }
}
