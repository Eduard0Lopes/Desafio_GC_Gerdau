package com.gerdau.desafio.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normalização textual idêntica à de {@code IA/src/normalizacao.py}:
 * sem acentos, minúsculas, apenas [a-z0-9] e espaços simples.
 */
public final class TextoUtil {

    public static final int LIMITE_SAP = 40;

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NAO_ALFANUMERICO = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private TextoUtil() {
    }

    public static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String semAcento = DIACRITICOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        String limpo = NAO_ALFANUMERICO.matcher(semAcento.toLowerCase()).replaceAll(" ");
        return ESPACOS.matcher(limpo).replaceAll(" ").trim();
    }

    public static String limpar(String texto) {
        return texto == null ? null : ESPACOS.matcher(texto).replaceAll(" ").trim();
    }
}
