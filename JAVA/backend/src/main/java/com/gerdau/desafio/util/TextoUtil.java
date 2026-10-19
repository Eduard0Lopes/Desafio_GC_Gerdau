package com.gerdau.desafio.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TextoUtil {

    public static final int LIMITE_SAP = 40;

    private static final Pattern NAO_ALFANUMERICO = Pattern.compile("[^a-z0-9\\s]");

    private static final Pattern ESPACOS_DUPLICADOS = Pattern.compile("\\s+");

    private TextoUtil() {
        throw new UnsupportedOperationException("Classe utilitária — não instancie.");
    }

    public static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        String limpo = NAO_ALFANUMERICO.matcher(semAcentoMinusculo(texto)).replaceAll(" ");

        return ESPACOS_DUPLICADOS.matcher(limpo).replaceAll(" ").trim();
    }

    /**
     * Reduz uma palavra ao singular provável em português (tolerância a plural).
     * Ex.: "telhados" → "telhado", "solucoes" → "solucao", "papeis" → "papel".
     * Palavras curtas, com dígitos ou com pontuação passam intactas.
     */
    public static String singularizar(String palavra) {
        if (palavra == null || palavra.isBlank()) {
            return "";
        }
        String p = semAcentoMinusculo(palavra.trim());
        if (p.length() < 4 || !soLetras(p)) {
            return p;
        }
        if (p.endsWith("oes") && p.length() >= 5) return p.substring(0, p.length() - 3) + "ao"; // solucoes → solucao
        if (p.endsWith("ns")) return p.substring(0, p.length() - 2) + "m";                       // bens → bem
        if (p.endsWith("zes") && p.length() >= 5) return p.substring(0, p.length() - 3) + "z";   // luzes → luz
        if (p.endsWith("eis") && p.length() >= 6) return p.substring(0, p.length() - 3) + "el";  // papeis → papel
        if (p.endsWith("ais") && p.length() >= 6) return p.substring(0, p.length() - 3) + "al";  // materiais → material
        if (p.endsWith("ois") && p.length() >= 5) return p.substring(0, p.length() - 3) + "ol";  // farois → farol
        if (p.endsWith("is") && p.length() >= 5) return p.substring(0, p.length() - 2) + "il";   // civis → civil
        if (p.endsWith("ores") && p.length() >= 6) return p.substring(0, p.length() - 2);        // tratores → trator
        if (p.endsWith("s") && !p.endsWith("ss")) return p.substring(0, p.length() - 1);         // telhados → telhado
        return p;
    }

    /**
     * Formas de uma palavra que devem casar na busca: como digitada, normalizada,
     * no singular e nos plurais prováveis.
     */
    public static Set<String> variantesPalavra(String palavra) {
        Set<String> formas = new LinkedHashSet<>();
        if (palavra == null || palavra.isBlank()) {
            return formas;
        }
        String digitada = palavra.trim();
        formas.add(digitada);

        String norm = semAcentoMinusculo(digitada);
        if (!norm.isEmpty() && norm.indexOf(' ') < 0) {
            formas.add(norm);
        }
        if (soLetras(norm)) {
            String singular = singularizar(norm);
            formas.add(singular);
            formas.addAll(plurais(singular));
        }
        return formas;
    }

    /**
     * Padrão de regex (case-insensitive) tolerante a plural/singular e acentos.
     * Cada palavra da consulta vira uma alternância entre suas formas prováveis e
     * precisa aparecer no texto em qualquer ordem (lookaheads), como uma busca comum.
     */
    public static String padraoBusca(String termo) {
        if (termo == null || termo.isBlank()) {
            return "";
        }
        String[] palavras = termo.trim().split("\\s+");
        List<String> partes = new ArrayList<>(palavras.length);
        for (String palavra : palavras) {
            List<String> formas = new ArrayList<>(variantesPalavra(palavra));
            String alternancia = formas.size() == 1
                    ? Pattern.quote(formas.get(0))
                    : "(?:" + formas.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")";
            partes.add("(?=.*" + alternancia + ")");
        }
        return "(?is)" + String.join("", partes);
    }

    private static List<String> plurais(String singular) {
        List<String> formas = new ArrayList<>();
        if (singular.length() < 4 || !soLetras(singular)) {
            return formas;
        }
        formas.add(singular + "s");
        char fim = singular.charAt(singular.length() - 1);
        if (singular.endsWith("ao")) {
            String base = singular.substring(0, singular.length() - 2);
            formas.add(base + "oes"); // acao → acoes
            formas.add(base + "aes"); // pao → paes
            formas.add(base + "os");  // mao → maos
        } else if (fim == 'm') {
            formas.add(singular.substring(0, singular.length() - 1) + "ns"); // bem → bens
        } else if (fim == 'z') {
            formas.add(singular + "zes"); // luz → luzes
        } else if (fim == 'r') {
            formas.add(singular + "es"); // trator → tratores
        } else if (singular.endsWith("il")) {
            // civil → civis; facil → faceis
            formas.add(singular.substring(0, singular.length() - 2) + "is");
            formas.add(singular.substring(0, singular.length() - 2) + "eis");
        } else if (singular.endsWith("al") || singular.endsWith("el")
                || singular.endsWith("ol") || singular.endsWith("ul")) {
            formas.add(singular.substring(0, singular.length() - 1) + "is"); // animal → animais; papel → papeis
        }
        return formas;
    }

    private static boolean soLetras(String texto) {
        return !texto.isEmpty() && texto.chars().allMatch(Character::isLetter);
    }

    private static String semAcentoMinusculo(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
    }

    public static String truncarParaSap(String texto) {
        if (texto == null) return null;
        if (texto.length() <= LIMITE_SAP) return texto;

        String cortado = texto.substring(0, LIMITE_SAP - 3);
        int ultimoEspaco = cortado.lastIndexOf(' ');
        if (ultimoEspaco > LIMITE_SAP / 2) {
            cortado = cortado.substring(0, ultimoEspaco);
        }
        return cortado + "...";
    }

    public static boolean excedeLimiteSap(String texto) {
        return texto != null && texto.length() > LIMITE_SAP;
    }

    public static String extrairDescricaoUtil(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return "";
        }

        String[] partes = snapshot.split("\\|");
        if (partes.length >= 3) {
            return partes[2].trim();
        }
        return snapshot.trim();
    }

    public static boolean saoIguaisNormalizados(String a, String b) {
        return normalizar(a).equals(normalizar(b));
    }

    /**
     * Núcleo "rígido" de comparação: remove o que é só enfeite — acentos, pontuação,
     * siglas/ligações curtas ("SV", "UN", "de"), plural, letras repetidas
     * ("telhadosss" → "telhados") e numeração colada na palavra ("teste1" → "teste").
     * Dois nomes com o mesmo núcleo são ESSENCIALMENTE o mesmo nome ("teste" vs
     * "SV - teste" vs "telhadosss"); só palavras reais a mais mudam o núcleo
     * ("Teste de algo").
     */
    public static String nucleo(String texto) {
        return String.join(" ", palavrasDoNucleo(texto));
    }

    /**
     * As palavras do núcleo, em ordem estável (ver {@link #nucleo(String)}).
     */
    public static List<String> palavrasDoNucleo(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) {
            return List.of();
        }
        List<String> partes = new ArrayList<>();
        for (String token : normalizado.split(" ")) {
            boolean temDigito = token.chars().anyMatch(Character::isDigit);
            if (!temDigito && token.length() <= 2) {
                continue; // siglas e ligações: "sv", "un", "de", "da"
            }
            String base = token;
            if (temDigito) {
                String semDigitos = token.replaceAll("\\d", "");
                // "teste1" → "teste"; mas códigos curtos como "m2"/"v8" são diferenças
                // reais (medidas) e ficam intactos
                if (semDigitos.length() >= 3 && semDigitos.length() < token.length()) {
                    base = semDigitos;
                }
            }
            // letras repetidas são enfeite: "telhadosss" → "telhados"
            base = colapsarRepeticoes(base);
            if (!base.isEmpty()) {
                partes.add(singularizar(base));
            }
        }
        if (partes.isEmpty()) {
            return List.of();
        }
        partes.sort(String::compareTo);
        return partes;
    }

    private static String colapsarRepeticoes(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        char anterior = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c != anterior) {
                sb.append(c);
            }
            anterior = c;
        }
        return sb.toString();
    }

    /**
     * As palavras do núcleo SEM qualquer conteúdo numérico ("telhado 2", "telhado2"
     * e "m2" sobram só o que é palavra). Número é medida — colado ou com espaço,
     * não muda a essência do nome; quem decide são as palavras.
     */
    public static List<String> palavrasDoNucleoSemNumeros(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) {
            return List.of();
        }
        List<String> partes = new ArrayList<>();
        for (String token : normalizado.split(" ")) {
            String semDigitos = token.replaceAll("\\d", "");
            if (semDigitos.isEmpty() || semDigitos.length() <= 2) {
                continue; // números puros, medidas curtas ("m2") e siglas ("sv")
            }
            partes.add(singularizar(colapsarRepeticoes(semDigitos)));
        }
        if (partes.isEmpty()) {
            return List.of();
        }
        partes.sort(String::compareTo);
        return partes;
    }

    /**
     * "Essencialmente iguais": o núcleo é o mesmo — a diferença é só numeração colada,
     * sigla, pontuação, plural ou letras repetidas ("teste" vs "teste1" vs
     * "SV - teste" vs "telhadosss"). Nomes com palavras reais a mais ("Teste de algo")
     * não são essencialmente iguais.
     */
    public static boolean saoEssencialmenteIguais(String a, String b) {
        List<String> nucleoA = palavrasDoNucleo(a);
        return !nucleoA.isEmpty() && nucleoA.equals(palavrasDoNucleo(b));
    }
}