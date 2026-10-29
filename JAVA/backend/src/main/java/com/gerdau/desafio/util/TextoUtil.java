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

    public static String singularizar(String palavra) {
        if (palavra == null || palavra.isBlank()) {
            return "";
        }
        String p = semAcentoMinusculo(palavra.trim());
        if (p.length() < 4 || !soLetras(p)) {
            return p;
        }
        if (p.endsWith("oes") && p.length() >= 5) return p.substring(0, p.length() - 3) + "ao";
        if (p.endsWith("ns")) return p.substring(0, p.length() - 2) + "m";
        if (p.endsWith("zes") && p.length() >= 5) return p.substring(0, p.length() - 3) + "z";
        if (p.endsWith("eis") && p.length() >= 6) return p.substring(0, p.length() - 3) + "el";
        if (p.endsWith("ais") && p.length() >= 6) return p.substring(0, p.length() - 3) + "al";
        if (p.endsWith("ois") && p.length() >= 5) return p.substring(0, p.length() - 3) + "ol";
        if (p.endsWith("is") && p.length() >= 5) return p.substring(0, p.length() - 2) + "il";
        if (p.endsWith("ores") && p.length() >= 6) return p.substring(0, p.length() - 2);
        if (p.endsWith("s") && !p.endsWith("ss")) return p.substring(0, p.length() - 1);
        return p;
    }

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
            formas.add(base + "oes");
            formas.add(base + "aes");
            formas.add(base + "os");
        } else if (fim == 'm') {
            formas.add(singular.substring(0, singular.length() - 1) + "ns");
        } else if (fim == 'z') {
            formas.add(singular + "zes");
        } else if (fim == 'r') {
            formas.add(singular + "es");
        } else if (singular.endsWith("il")) {
            formas.add(singular.substring(0, singular.length() - 2) + "is");
            formas.add(singular.substring(0, singular.length() - 2) + "eis");
        } else if (singular.endsWith("al") || singular.endsWith("el")
                || singular.endsWith("ol") || singular.endsWith("ul")) {
            formas.add(singular.substring(0, singular.length() - 1) + "is");
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

    public static String nvl(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    public static boolean vazio(String valor) {
        return !preenchido(valor);
    }

    public static boolean excedeLimiteSap(String texto) {
        return texto != null && texto.length() > LIMITE_SAP;
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

    public static List<String> palavrasDoNucleoSemNumeros(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) {
            return List.of();
        }
        List<String> partes = new ArrayList<>();
        for (String token : normalizado.split(" ")) {
            String semDigitos = token.replaceAll("\\d", "");
            if (semDigitos.isEmpty() || semDigitos.length() <= 2) {
                continue;
            }
            partes.add(singularizar(colapsarRepeticoes(semDigitos)));
        }
        if (partes.isEmpty()) {
            return List.of();
        }
        partes.sort(String::compareTo);
        return partes;
    }
}
