package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.ComponenteDtos.ComponenteDto;
import br.com.gerdau.servicos.api.persistence.ComponenteRepository;
import br.com.gerdau.servicos.api.search.SearchClient;
import br.com.gerdau.servicos.api.search.SearchHit;
import br.com.gerdau.servicos.api.text.SimilarityScorer;
import br.com.gerdau.servicos.api.text.TextNormalizer;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static br.com.gerdau.servicos.api.persistence.DocumentSupport.id;

/**
 * Busca por ID, nome ou descrição (doc. 6.6/6.7). Ranking: ID exato > prefixo de ID > texto exato >
 * prefixo de texto > palavras por prefixo > fuzzy (aproximado).
 */
@Service
public class ComponenteService {

    private static final Pattern ID_LIKE = Pattern.compile("^[A-Za-z0-9._-]{1,32}$");
    private static final double MIN_FUZZY_RANK = 240; // 0,60 de similaridade

    private final ComponenteRepository repo;
    private final SearchClient search;
    private final TextNormalizer normalizer;
    private final SimilarityScorer scorer;
    private final ComponenteMapper mapper;

    public ComponenteService(ComponenteRepository repo, SearchClient search, TextNormalizer normalizer,
                             SimilarityScorer scorer, ComponenteMapper mapper) {
        this.repo = repo;
        this.search = search;
        this.normalizer = normalizer;
        this.scorer = scorer;
        this.mapper = mapper;
    }

    private record Ranked(Document doc, double rank, boolean aproximado) {}

    public List<ComponenteDto> buscar(TipoComponente tipo, String query, int limit) {
        String raw = query == null ? "" : query.trim();
        if (normalizer.fold(raw).isEmpty()) {
            return repo.listFirst(tipo, limit).stream().map(d -> mapper.toDto(tipo, d, null, false)).toList();
        }
        String normalized = normalizer.normalize(raw);
        List<String> prefixTokens = normalizer.prefixTokens(raw);
        Map<String, Ranked> ranked = new LinkedHashMap<>();

        if (ID_LIKE.matcher(raw).matches() && raw.chars().anyMatch(Character::isDigit)) {
            repo.findById(tipo, raw).ifPresent(d -> ranked.put(id(d), new Ranked(d, 1000, false)));
            for (Document d : repo.findByIdPrefix(tipo, raw, limit)) {
                ranked.putIfAbsent(id(d), new Ranked(d, 900, false));
            }
        }
        for (SearchHit hit : search.autocomplete(tipo, prefixTokens, limit * 2)) {
            ranked.putIfAbsent(hit.id(), rank(tipo, normalized, prefixTokens, hit.document()));
        }
        // Poucos resultados: tenta tolerar erro de digitação (fuzzy) com candidatos mais amplos.
        if (ranked.size() < limit && normalized.length() >= 3) {
            List<String> tokens = normalizer.contentTokens(normalized);
            for (SearchHit hit : search.candidates(tipo, tokens, limit * 5)) {
                if (ranked.containsKey(hit.id())) continue;
                Ranked r = rank(tipo, normalized, prefixTokens, hit.document());
                if (r.rank() >= MIN_FUZZY_RANK) ranked.put(hit.id(), r);
            }
        }
        return ranked.values().stream()
                .sorted(Comparator.comparingDouble(Ranked::rank).reversed())
                .limit(limit)
                .map(r -> mapper.toDto(tipo, r.doc(), Math.round(r.rank() / 10.0) / 100.0, r.aproximado()))
                .toList();
    }

    private Ranked rank(TipoComponente tipo, String normalizedQuery, List<String> prefixTokens, Document doc) {
        String norm = mapper.normalizedOf(tipo, doc);
        if (norm.equals(normalizedQuery)) return new Ranked(doc, 800, false);
        if (norm.startsWith(String.join(" ", prefixTokens))) return new Ranked(doc, 700, false);
        List<String> docTokens = normalizer.contentTokens(norm);
        boolean allPrefixes = prefixTokens.stream()
                .allMatch(p -> docTokens.stream().anyMatch(t -> t.startsWith(p)));
        if (allPrefixes) return new Ranked(doc, 600, false);
        return new Ranked(doc, 400 * scorer.score(normalizedQuery, norm).score(), true);
    }
}
