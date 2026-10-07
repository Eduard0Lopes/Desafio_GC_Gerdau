package br.com.gerdau.servicos.api.text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calcula a similaridade (0..1) entre dois textos JÁ normalizados e explica o motivo.
 * Combina: igualdade, mesmas palavras em outra ordem, flexão/abreviação (stemming),
 * sobreposição suave de palavras e distância de edição. Números diferentes limitam a nota
 * (ex.: "mecânico 2 pessoas" x "mecânico 3 pessoas" são parecidos, mas não duplicados).
 */
public final class SimilarityScorer {

    public enum Motivo {
        IDENTICO("Descrição idêntica após normalização (acentos, caixa e pontuação)"),
        MESMAS_PALAVRAS("Mesmas palavras em ordem diferente"),
        VARIACAO_FLEXAO("Mesmas palavras com variação de flexão ou abreviação"),
        TEXTO_PROXIMO("Texto muito parecido (possível erro de digitação)"),
        PALAVRAS_EM_COMUM("Várias palavras em comum"),
        SEM_RELACAO("Pouca relação entre os textos");

        private final String descricao;

        Motivo(String descricao) { this.descricao = descricao; }

        public String descricao() { return descricao; }
    }

    public record Resultado(double score, Motivo motivo) {}

    private final TextNormalizer normalizer;
    private final PortugueseStemmer stemmer = new PortugueseStemmer();
    private final double numericCap;

    /** @param numericCap nota máxima quando os números dos textos diferem (use limiar de bloqueio - 0,01). */
    public SimilarityScorer(TextNormalizer normalizer, double numericCap) {
        this.normalizer = normalizer;
        this.numericCap = numericCap;
    }

    public Resultado score(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return new Resultado(0.0, Motivo.SEM_RELACAO);
        }
        if (a.equals(b)) {
            return new Resultado(1.0, Motivo.IDENTICO);
        }
        List<String> ta = normalizer.contentTokens(a);
        List<String> tb = normalizer.contentTokens(b);

        double score;
        Motivo motivo;
        if (sorted(ta).equals(sorted(tb))) {
            score = 0.97;
            motivo = Motivo.MESMAS_PALAVRAS;
        } else {
            List<String> sa = ta.stream().map(stemmer::stem).sorted().toList();
            List<String> sb = tb.stream().map(stemmer::stem).sorted().toList();
            if (sa.equals(sb)) {
                score = 0.95;
                motivo = Motivo.VARIACAO_FLEXAO;
            } else {
                double soft = softOverlap(sa, sb);
                double chars = Levenshtein.similarity(String.join(" ", sa), String.join(" ", sb));
                score = Math.max(soft, chars);
                if (chars >= soft) {
                    motivo = chars >= 0.80 ? Motivo.TEXTO_PROXIMO : Motivo.SEM_RELACAO;
                } else {
                    motivo = soft >= 0.50 ? Motivo.PALAVRAS_EM_COMUM : Motivo.SEM_RELACAO;
                }
            }
        }
        if (!numbers(ta).equals(numbers(tb))) {
            score = Math.min(score, numericCap);
        }
        return new Resultado(score, motivo);
    }

    private static List<String> sorted(List<String> tokens) {
        return tokens.stream().sorted().toList();
    }

    private static Set<String> numbers(List<String> tokens) {
        Set<String> out = new HashSet<>();
        for (String t : tokens) {
            if (PortugueseStemmer.isNumeric(t)) out.add(t);
        }
        return out;
    }

    private static double softOverlap(List<String> a, List<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        double sum = 0;
        for (String x : a) sum += best(x, b);
        for (String y : b) sum += best(y, a);
        return sum / (a.size() + b.size());
    }

    private static double best(String token, List<String> others) {
        double best = 0;
        for (String o : others) {
            best = Math.max(best, tokenSimilarity(token, o));
        }
        return best;
    }

    private static double tokenSimilarity(String x, String y) {
        if (x.equals(y)) return 1.0;
        if (PortugueseStemmer.isNumeric(x) || PortugueseStemmer.isNumeric(y)) return 0.0;
        if (Math.min(x.length(), y.length()) < 4) return 0.0;
        double s = Levenshtein.similarity(x, y);
        return s >= 0.75 ? s : 0.0;
    }
}
