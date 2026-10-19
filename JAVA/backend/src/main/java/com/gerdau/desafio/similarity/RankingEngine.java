package com.gerdau.desafio.similarity;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.util.TextoUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RankingEngine {

    private RankingEngine() {
        throw new UnsupportedOperationException("Classe utilitária — não instancie.");
    }

    public static CatalogoItem prepararItem(String id, String codigoOriginal, List<String> legacyIds,
                                            String descricao, String descricaoNormalizada) {
        return prepararItem(id, codigoOriginal, legacyIds, descricao, descricaoNormalizada, false, null);
    }

    public static CatalogoItem prepararItem(String id, String codigoOriginal, List<String> legacyIds,
                                            String descricao, String descricaoNormalizada,
                                            boolean casoIsolado, String motivoCasoIsolado) {

        String base = (descricaoNormalizada == null || descricaoNormalizada.isBlank())
                ? descricao : descricaoNormalizada;
        String normalizada = TextoUtil.normalizar(base == null ? "" : base);
        return new CatalogoItem(id, codigoOriginal,
                legacyIds == null ? List.of() : legacyIds,
                descricao, normalizada, SimilarityEngine.tokens(normalizada), SimilarityEngine.numeros(normalizada),
                casoIsolado, motivoCasoIsolado);
    }

    public static List<CandidatoSimilarResponse> ranquear(String consulta, List<CatalogoItem> catalogo,
                                                          int limite, double scoreMinimo) {
        String consultaNormalizada = TextoUtil.normalizar(consulta);
        String consultaBruta = consulta == null ? "" : consulta.trim();
        List<String> palavrasConsulta = TextoUtil.palavrasDoNucleoSemNumeros(consulta);
        Set<String> tokensConsulta = SimilarityEngine.tokens(consultaNormalizada);
        List<String> numerosConsulta = SimilarityEngine.numeros(consultaNormalizada);
        // palavras incomuns no catálogo: uma delas casando sozinha já vale um alerta
        Set<String> palavrasEspecificas = palavrasEspecificas(palavrasConsulta, catalogo);

        List<Pontuado> pontuados = new ArrayList<>();
        for (CatalogoItem item : catalogo) {
            boolean idIgual = !consultaBruta.isEmpty()
                    && (consultaBruta.equals(item.codigoOriginal()) || item.legacyIds().contains(consultaBruta)
                    || consultaBruta.equals(item.id()));
            boolean textoIgual = consultaNormalizada.equals(item.descricaoNormalizada());
            List<String> palavrasItem = TextoUtil.palavrasDoNucleoSemNumeros(item.descricao());
            // números são MEDIDA, não palavra — colado ("telhado2") ou com espaço
            // ("telhado 2") tem o mesmo tratamento: número a mais só de um lado não
            // muda o nome; números diferentes dos dois lados são medidas diferentes
            boolean medidasDiferentes = !numerosConsulta.isEmpty() && !item.numeros().isEmpty()
                    && !numerosConsulta.equals(item.numeros());
            boolean essencialmenteIgual = !medidasDiferentes && !palavrasConsulta.isEmpty()
                    && (palavrasConsulta.equals(palavrasItem)
                    || mesmaEssenciaPalavraAPalavra(palavrasConsulta, palavrasItem));
            // 1 palavra completa em comum (aceitando erro de digitação) já gera ALERTA:
            // descrições longas não podem "diluir" o match de uma palavra que casa
            String palavraEmComum = null;
            if (!essencialmenteIgual) {
                for (String palavra : palavrasConsulta) {
                    if (!palavrasEspecificas.contains(palavra)) {
                        continue; // palavra genérica não dispara alerta sozinha
                    }
                    if (palavrasItem.stream().anyMatch(p -> palavraEssencialmenteIgual(palavra, p))) {
                        palavraEmComum = palavra;
                        break;
                    }
                }
            }

            double tokens = SimilarityEngine.jaccard(tokensConsulta, item.tokens());
            double caracteres = (tokensConsulta.isEmpty() || item.tokens().isEmpty())
                    ? 0.0
                    : SimilarityEngine.razaoCaracteres(consultaNormalizada, item.descricaoNormalizada());
            double score = (tokensConsulta.isEmpty() || item.tokens().isEmpty())
                    ? 0.0
                    : tokens * SimilarityEngine.PESO_TOKENS + caracteres * SimilarityEngine.PESO_CARACTERES;

            boolean limitadoPorNumeros = !idIgual && !essencialmenteIgual && !numerosConsulta.equals(item.numeros())
                    && score > SimilarityEngine.TETO_NUMEROS_DIFERENTES;
            if (limitadoPorNumeros) {
                score = SimilarityEngine.TETO_NUMEROS_DIFERENTES;
            }
            if (idIgual) {
                score = 1.0;
            }
            if (essencialmenteIgual) {
                // mesma essência: duplicidade garantida, mesmo com grafia/numeração diferente
                score = 1.0;
            }
            if (palavraEmComum != null && score < PISO_ALERTA_PALAVRA_ESPECIFICA) {
                // palavra específica casando nunca fica abaixo do nível de alerta
                score = PISO_ALERTA_PALAVRA_ESPECIFICA;
            }
            if (score >= scoreMinimo) {
                pontuados.add(new Pontuado(item, score, tokens, caracteres, idIgual, textoIgual, essencialmenteIgual,
                        limitadoPorNumeros, palavraEmComum));
            }
        }

        pontuados.sort(Comparator.comparingDouble(Pontuado::score).reversed()
                .thenComparing(p -> p.item().id(), Comparator.nullsLast(Comparator.naturalOrder())));

        List<CandidatoSimilarResponse> resultado = new ArrayList<>();
        int posicao = 1;
        for (Pontuado p : pontuados) {
            if (posicao > limite) {
                break;
            }
            List<String> emComum = tokensConsulta.stream().filter(p.item().tokens()::contains).toList();
            resultado.add(new CandidatoSimilarResponse(
                    posicao++,
                    p.item().id(),
                    p.item().codigoOriginal(),
                    p.item().descricao(),
                    arredondar(p.score()),
                    arredondar(p.tokens()),
                    arredondar(p.caracteres()),
                    emComum,
                    motivo(p, emComum),
                    p.item().casoIsolado(),
                    p.item().motivoCasoIsolado()));
        }
        return resultado;
    }

    private static final double SEMELHANCA_PALAVRA_IGUAL = 0.80;
    private static final int APENDICE_TOLERADO = 3;
    /** nível mínimo do score quando uma palavra específica casa (0.70 = ALERTA) */
    private static final double PISO_ALERTA_PALAVRA_ESPECIFICA = 0.70;
    /** uma palavra que aparece em mais de 5% do catálogo (mín. 2 itens) é genérica */
    private static final double FRACAO_PALAVRA_GENERICA = 0.05;

    /**
     * Palavras incomuns no catálogo: só elas disparam o alerta por palavra única —
     * palavras genéricas ("manutenção", "serviço") aparecem em muitos itens e
     * sozinhas não dizem nada.
     */
    private static Set<String> palavrasEspecificas(List<String> palavrasConsulta, List<CatalogoItem> catalogo) {
        Set<String> especificas = new LinkedHashSet<>();
        for (String palavra : palavrasConsulta) {
            long frequencia = 0;
            for (CatalogoItem item : catalogo) {
                if (contemPalavraEssencial(item, palavra)) {
                    frequencia++;
                }
            }
            long limite = Math.max(2L, (long) (catalogo.size() * FRACAO_PALAVRA_GENERICA));
            if (frequencia <= limite) {
                especificas.add(palavra);
            }
        }
        return especificas;
    }

    private static boolean contemPalavraEssencial(CatalogoItem item, String palavra) {
        for (String p : TextoUtil.palavrasDoNucleoSemNumeros(item.descricao())) {
            if (palavraEssencialmenteIgual(palavra, p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cada par de palavras precisa ser essencialmente a mesma ("telhados" vs
     * "telhadoa"/"tehado"/"telhadoo"). Palavras curtas precisam casar exato
     * ("m2" vs "m3" são medidas diferentes).
     */
    private static boolean mesmaEssenciaPalavraAPalavra(List<String> a, List<String> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!palavraEssencialmenteIgual(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean palavraEssencialmenteIgual(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        int menor = Math.min(a.length(), b.length());
        int maior = Math.max(a.length(), b.length());
        // uma palavra é a outra com um apêndice curto ("telhado" vs "telhadoxyz")
        if (maior - menor <= APENDICE_TOLERADO && (a.startsWith(b) || b.startsWith(a))) {
            return true;
        }
        if (menor < 5) {
            return false;
        }
        // digitação: uma letra a mais/menos/trocada ("tehado", "telhadoa")
        return 1.0 - (double) distanciaEdicao(a, b) / maior >= SEMELHANCA_PALAVRA_IGUAL;
    }

    private static int distanciaEdicao(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] atual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(atual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + custo);
            }
            int[] troca = anterior;
            anterior = atual;
            atual = troca;
        }
        return anterior[b.length()];
    }

    private static String motivo(Pontuado p, List<String> emComum) {
        if (p.idIgual()) {
            return "O texto informado é igual ao código (ou código legado) deste item.";
        }
        if (p.textoIgual()) {
            return "Mesma descrição após normalização (minúsculas, sem acentos e sem pontuação).";
        }
        if (p.essencialmenteIgual()) {
            return "Essencialmente o mesmo nome: a diferença é só numeração (colada ou com espaço), sigla, "
                    + "pontuação, plural ou letras repetidas/trocadas (ex.: 'teste' vs 'teste1' vs 'teste 2', "
                    + "'SV - teste', 'telhados' vs 'telhadosss'). Reutilize este item ou justifique.";
        }
        String base;
        if (p.palavraEmComum() != null) {
            base = "A palavra \"" + p.palavraEmComum() + "\" aparece neste item (aceitando pequenos erros de "
                    + "digitação, ex.: 'telado' x 'telhado') — confirme se é o mesmo serviço antes de criar.";
        } else {
            String comuns = emComum.isEmpty() ? "nenhuma palavra em comum" : "palavras em comum: " + String.join(", ", emComum);
            base = String.format(Locale.ROOT, "%s; semelhança de caracteres %.0f%%; palavras %.0f%%.",
                    comuns, p.caracteres() * 100, p.tokens() * 100);
        }
        return p.limitadoPorNumeros()
                ? base + " Os números/medidas são diferentes, então o score foi limitado a 85% (não é duplicidade automática)."
                : base;
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10000.0) / 10000.0;
    }

    private record Pontuado(CatalogoItem item, double score, double tokens, double caracteres,
                            boolean idIgual, boolean textoIgual, boolean essencialmenteIgual,
                            boolean limitadoPorNumeros, String palavraEmComum) {
    }
}
