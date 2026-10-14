package com.gerdau.desafio.service.similaridade;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Espelho de {@code IA/src/fuzzy_engine.py}: 40% Jaccard de tokens + 60% razão do
 * {@code difflib.SequenceMatcher} do Python (Ratcliff/Obershelp, sem junk).
 */
public final class FuzzyEngine {

    private static final double PESO_TOKENS = 0.4;
    private static final double PESO_SEQUENCIA = 0.6;

    private FuzzyEngine() {
    }

    public static double similaridade(String a, String b) {
        Set<String> tokensA = tokens(a);
        Set<String> tokensB = tokens(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersecao = new HashSet<>(tokensA);
        intersecao.retainAll(tokensB);
        Set<String> uniao = new HashSet<>(tokensA);
        uniao.addAll(tokensB);

        double jaccard = (double) intersecao.size() / uniao.size();
        return jaccard * PESO_TOKENS + razaoSequencia(a, b) * PESO_SEQUENCIA;
    }

    public static double razaoSequencia(String a, String b) {
        int total = a.length() + b.length();
        if (total == 0) {
            return 1.0;
        }
        return 2.0 * totalCaracteresCoincidentes(a, b) / total;
    }

    private static Set<String> tokens(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(texto.trim().split("\\s+")));
    }

    private static int totalCaracteresCoincidentes(String a, String b) {
        int total = 0;
        Deque<int[]> fila = new ArrayDeque<>();
        fila.push(new int[]{0, a.length(), 0, b.length()});
        while (!fila.isEmpty()) {
            int[] faixa = fila.pop();
            int alo = faixa[0], ahi = faixa[1], blo = faixa[2], bhi = faixa[3];
            int[] match = maiorBlocoComum(a, b, alo, ahi, blo, bhi);
            int i = match[0], j = match[1], k = match[2];
            if (k == 0) {
                continue;
            }
            total += k;
            if (alo < i && blo < j) {
                fila.push(new int[]{alo, i, blo, j});
            }
            if (i + k < ahi && j + k < bhi) {
                fila.push(new int[]{i + k, ahi, j + k, bhi});
            }
        }
        return total;
    }

    private static int[] maiorBlocoComum(String a, String b, int alo, int ahi, int blo, int bhi) {
        int melhorI = alo, melhorJ = blo, melhorTamanho = 0;
        int[] anterior = new int[b.length() + 1];
        for (int i = alo; i < ahi; i++) {
            int[] atual = new int[b.length() + 1];
            char c = a.charAt(i);
            for (int j = blo; j < bhi; j++) {
                if (b.charAt(j) == c) {
                    int k = anterior[j] + 1;
                    atual[j + 1] = k;
                    if (k > melhorTamanho) {
                        melhorI = i - k + 1;
                        melhorJ = j - k + 1;
                        melhorTamanho = k;
                    }
                }
            }
            anterior = atual;
        }
        return new int[]{melhorI, melhorJ, melhorTamanho};
    }
}
