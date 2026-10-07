package br.com.gerdau.servicos.api.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Normalização de texto (doc. seção 6.5). Classe pura (sem Spring) para ser fácil de testar.
 * O texto ORIGINAL nunca é alterado: guarde-o em "descricao"/"nome" e use o normalizado só para busca.
 */
public final class TextNormalizer {

    private static final Set<String> STOPWORDS = Set.of(
            "de", "da", "do", "das", "dos", "e", "a", "o", "as", "os",
            "para", "em", "no", "na", "nos", "nas", "com", "por", "ao");

    private final Map<String, String> abbreviations;

    public TextNormalizer(Map<String, String> abbreviations) {
        this.abbreviations = Map.copyOf(abbreviations);
    }

    public static TextNormalizer fromProperties(InputStream in) throws IOException {
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        Map<String, String> map = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            map.put(key.trim().toLowerCase(Locale.ROOT), props.getProperty(key).trim().toLowerCase(Locale.ROOT));
        }
        return new TextNormalizer(map);
    }

    /** Minúsculas, sem acentos, pontuação/separadores uniformizados em espaço único. Números são preservados. */
    public String fold(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** fold + expansão de abreviações conhecidas + remoção de palavras vazias (de, da, para...). */
    public String normalize(String raw) {
        String folded = fold(raw);
        if (folded.isEmpty()) return "";
        StringBuilder expanded = new StringBuilder(folded.length() + 16);
        for (String token : folded.split(" ")) {
            if (expanded.length() > 0) expanded.append(' ');
            expanded.append(abbreviations.getOrDefault(token, token));
        }
        return String.join(" ", contentTokens(expanded.toString()));
    }

    /** Tokens sem palavras vazias (se só sobrarem palavras vazias, mantém todas). */
    public List<String> contentTokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> all = Arrays.asList(text.trim().split("\\s+"));
        List<String> filtered = all.stream().filter(t -> !STOPWORDS.contains(t)).toList();
        return filtered.isEmpty() ? List.copyOf(all) : filtered;
    }

    /**
     * Tokens para busca por prefixo enquanto o usuário digita. NÃO expande abreviações
     * (digitar "mec" deve casar mecânica e mecânico, não só "mecanica").
     */
    public List<String> prefixTokens(String raw) {
        return contentTokens(fold(raw));
    }
}
