package br.com.gerdau.servicos.api.text;

/**
 * Stemmer leve para português, pensado para texto JÁ normalizado (minúsculo, sem acento).
 * Não substitui um analisador linguístico completo; serve para aproximar flexões comuns
 * (mecânico/mecânica, manutenção/manutenções, serviço/serviços).
 */
public final class PortugueseStemmer {

    public String stem(String word) {
        if (word == null || word.length() <= 3 || isNumeric(word)) {
            return word;
        }
        String w = word;
        if (w.endsWith("oes") || w.endsWith("aes") || w.endsWith("aos")) {
            w = w.substring(0, w.length() - 3) + "ao";
        } else if (w.endsWith("ais")) {
            w = w.substring(0, w.length() - 3) + "al";
        } else if (w.endsWith("eis")) {
            w = w.substring(0, w.length() - 3) + "el";
        } else if (w.endsWith("res") || w.endsWith("zes")) {
            w = w.substring(0, w.length() - 2);
        } else if (w.endsWith("ns")) {
            w = w.substring(0, w.length() - 2) + "m";
        } else if (w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us") && !w.endsWith("is")) {
            w = w.substring(0, w.length() - 1);
        }
        if (w.length() > 4 && "aeo".indexOf(w.charAt(w.length() - 1)) >= 0) {
            w = w.substring(0, w.length() - 1);
        }
        return w;
    }

    static boolean isNumeric(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}
