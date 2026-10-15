package com.gerdau.desafio.similarity;

/**
 * Política de decisão a partir do melhor score (documento técnico, seção 6.7).
 * Limiares calibrados na trilha IA ({@code IA/src/ranking_policy.py}):
 * >= 0,88 duplicidade; >= 0,70 alerta.
 */
public class RankingPolicy {

    public static final String DUPLICIDADE = "DUPLICIDADE";
    public static final String ALERTA = "ALERTA";
    public static final String ORIGINAL = "ORIGINAL";

    public record Decisao(String status, String decisao, String acaoRecomendada) {
    }

    private final double limiarDuplicidade;
    private final double limiarAlerta;

    public RankingPolicy(double limiarDuplicidade, double limiarAlerta) {
        if (limiarAlerta > limiarDuplicidade) {
            throw new IllegalArgumentException("O limiar de alerta não pode ser maior que o de duplicidade.");
        }
        this.limiarDuplicidade = limiarDuplicidade;
        this.limiarAlerta = limiarAlerta;
    }

    public Decisao aplicar(double melhorScore) {
        if (melhorScore >= limiarDuplicidade) {
            return new Decisao(DUPLICIDADE, "REVISAR",
                    "Provável duplicidade: reutilizar o código existente ou justificar a criação.");
        }
        if (melhorScore >= limiarAlerta) {
            return new Decisao(ALERTA, "ALERTAR",
                    "Há itens semelhantes: confirmar se algum deles atende antes de criar.");
        }
        return new Decisao(ORIGINAL, "PERMITIR",
                "Nenhum item semelhante relevante: pode seguir com o cadastro.");
    }

    public double getLimiarDuplicidade() {
        return limiarDuplicidade;
    }

    public double getLimiarAlerta() {
        return limiarAlerta;
    }
}
