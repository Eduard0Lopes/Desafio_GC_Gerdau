package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.config.AppProperties;
import br.com.gerdau.servicos.api.domain.DecisaoSimilaridade;
import br.com.gerdau.servicos.api.domain.SimilarityPolicy;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilarItem;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilaridadeResponse;
import br.com.gerdau.servicos.api.persistence.ComponenteRepository;
import br.com.gerdau.servicos.api.search.SearchClient;
import br.com.gerdau.servicos.api.search.SearchHit;
import br.com.gerdau.servicos.api.text.SimilarityScorer;
import br.com.gerdau.servicos.api.text.TextNormalizer;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static br.com.gerdau.servicos.api.persistence.DocumentSupport.id;
import static br.com.gerdau.servicos.api.persistence.DocumentSupport.str;

/**
 * Decide se um texto novo é parecido com algo que já existe. O mecanismo de busca só traz CANDIDATOS;
 * a nota (0..1), o motivo e a decisão são calculados aqui, de forma igual para qualquer motor.
 */
@Service
public class SimilarityService {

    /** Abaixo disso o item nem é listado como "semelhante". */
    private static final double MIN_REPORTED = 0.60;

    private final SearchClient search;
    private final ComponenteRepository repo;
    private final TextNormalizer normalizer;
    private final SimilarityScorer scorer;
    private final SimilarityPolicy policy;
    private final ComponenteMapper mapper;
    private final AppProperties props;

    public SimilarityService(SearchClient search, ComponenteRepository repo, TextNormalizer normalizer,
                             SimilarityScorer scorer, SimilarityPolicy policy, ComponenteMapper mapper,
                             AppProperties props) {
        this.search = search;
        this.repo = repo;
        this.normalizer = normalizer;
        this.scorer = scorer;
        this.policy = policy;
        this.mapper = mapper;
        this.props = props;
    }

    /** @param excluirId item a ignorar (o próprio item, quando já existe); pode ser null. */
    public SimilaridadeResponse avaliar(TipoComponente tipo, String texto, String excluirId) {
        String normalized = normalizer.normalize(texto);
        if (normalized.isEmpty()) {
            return resposta(0.0, List.of());
        }
        Map<String, Document> pool = new LinkedHashMap<>();
        for (Document d : repo.findByNormalizedText(tipo, normalized, 5)) {
            pool.putIfAbsent(id(d), d);
        }
        for (SearchHit hit : search.candidates(tipo, normalizer.contentTokens(normalized),
                props.similarity().candidatePool())) {
            pool.putIfAbsent(hit.id(), hit.document());
        }

        List<SimilarItem> itens = new ArrayList<>();
        for (Document d : pool.values()) {
            String itemId = id(d);
            if (itemId.equals(excluirId)) continue;
            SimilarityScorer.Resultado r = scorer.score(normalized, mapper.normalizedOf(tipo, d));
            double score = Math.round(r.score() * 100.0) / 100.0;
            if (score >= MIN_REPORTED) {
                String sigla = tipo == TipoComponente.UM ? str(d, "sigla") : null;
                itens.add(new SimilarItem(itemId, str(d, tipo.textField()), sigla, score, r.motivo().descricao()));
            }
        }
        itens.sort(Comparator.comparingDouble(SimilarItem::score).reversed());
        List<SimilarItem> top = itens.stream().limit(10).toList();
        return resposta(top.isEmpty() ? 0.0 : top.get(0).score(), top);
    }

    private SimilaridadeResponse resposta(double max, List<SimilarItem> itens) {
        DecisaoSimilaridade decisao = policy.decide(max);
        return new SimilaridadeResponse(decisao, max, props.similarity().enforcement().name(),
                policy.alertThreshold(), policy.blockThreshold(), max < 1.0 && !itens.isEmpty(), itens);
    }
}
