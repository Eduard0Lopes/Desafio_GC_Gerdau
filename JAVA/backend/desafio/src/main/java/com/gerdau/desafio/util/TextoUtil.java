package com.gerdau.desafio.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

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

        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String limpo = NAO_ALFANUMERICO.matcher(semAcento.toLowerCase()).replaceAll(" ");

        return ESPACOS_DUPLICADOS.matcher(limpo).replaceAll(" ").trim();
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
}