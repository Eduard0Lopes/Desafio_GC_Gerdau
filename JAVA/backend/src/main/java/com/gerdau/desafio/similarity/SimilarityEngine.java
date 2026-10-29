package com.gerdau.desafio.similarity;

import com.gerdau.desafio.util.TextoUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SimilarityEngine {

    public static final double PESO_TOKENS = 0.4;
    public static final double PESO_CARACTERES = 0.6;

    public static final double TETO_NUMEROS_DIFERENTES = 0.85;

    private SimilarityEngine() {
        throw new UnsupportedOperationException("Classe utilitária — não instancie.");
    }

    public static Set<String> tokens(String texto) {
        Set<String> resultado = new LinkedHashSet<>();
        if (texto == null || texto.isBlank()) {
            return resultado;
        }
        for (String token : texto.trim().split("\\s+")) {
            resultado.add(TextoUtil.singularizar(token));
        }
        return resultado;
    }

    public static double score(String a, String b) {
        return score(a, tokens(a), b, tokens(b));
    }

    public static double score(String a, Set<String> tokensA, String b, Set<String> tokensB) {
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        return jaccard(tokensA, tokensB) * PESO_TOKENS + razaoCaracteres(a, b) * PESO_CARACTERES;
    }

    public static List<String> numeros(String normalizado) {
        List<String> resultado = new ArrayList<>();
        if (normalizado == null || normalizado.isBlank()) {
            return resultado;
        }
        for (String token : normalizado.trim().split("\\s+")) {
            if (token.chars().anyMatch(Character::isDigit)) {
                resultado.add(token);
            }
        }
        return resultado;
    }

    public static double scoreComRegraNumerica(String a, String b) {
        double base = score(a, b);
        return numeros(a).equals(numeros(b)) ? base : Math.min(base, TETO_NUMEROS_DIFERENTES);
    }

    public static double jaccard(Set<String> tokensA, Set<String> tokensB) {
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        int intersecao = 0;
        for (String token : tokensA) {
            if (tokensB.contains(token)) {
                intersecao++;
            }
        }
        int uniao = tokensA.size() + tokensB.size() - intersecao;
        return uniao == 0 ? 0.0 : (double) intersecao / uniao;
    }

    public static double razaoCaracteres(String a, String b) {
        int total = a.length() + b.length();
        if (total == 0) {
            return 1.0;
        }
        int casados = caracteresCasados(a, b, 0, a.length(), 0, b.length());
        return 2.0 * casados / total;
    }

    private static int caracteresCasados(String a, String b, int alo, int ahi, int blo, int bhi) {
        int[] melhor = maiorBlocoComum(a, b, alo, ahi, blo, bhi);
        int i = melhor[0];
        int j = melhor[1];
        int k = melhor[2];
        if (k == 0) {
            return 0;
        }
        int total = k;
        if (alo < i && blo < j) {
            total += caracteresCasados(a, b, alo, i, blo, j);
        }
        if (i + k < ahi && j + k < bhi) {
            total += caracteresCasados(a, b, i + k, ahi, j + k, bhi);
        }
        return total;
    }

    private static int[] maiorBlocoComum(String a, String b, int alo, int ahi, int blo, int bhi) {
        int melhorI = alo;
        int melhorJ = blo;
        int melhorTamanho = 0;
        int[] anterior = new int[bhi - blo + 1];
        int[] atual = new int[bhi - blo + 1];
        for (int i = alo; i < ahi; i++) {
            java.util.Arrays.fill(atual, 0);
            char ca = a.charAt(i);
            for (int j = blo; j < bhi; j++) {
                if (ca == b.charAt(j)) {
                    int k = anterior[j - blo] + 1;
                    atual[j - blo + 1] = k;
                    if (k > melhorTamanho) {
                        melhorI = i - k + 1;
                        melhorJ = j - k + 1;
                        melhorTamanho = k;
                    }
                }
            }
            int[] troca = anterior;
            anterior = atual;
            atual = troca;
        }
        return new int[]{melhorI, melhorJ, melhorTamanho};
    }
}
