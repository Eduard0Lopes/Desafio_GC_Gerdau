package br.com.gerdau.servicos.api.domain;

/** Política de faixas de similaridade (doc. seção 6.7). Valores são ponto de partida: calibre com amostra real. */
public record SimilarityPolicy(double alertThreshold, double blockThreshold) {

    public SimilarityPolicy {
        if (!(alertThreshold > 0 && alertThreshold < blockThreshold && blockThreshold <= 1.0)) {
            throw new IllegalArgumentException(
                    "Limiares inválidos: é preciso 0 < alerta < bloqueio <= 1 (recebido alerta="
                            + alertThreshold + ", bloqueio=" + blockThreshold + ")");
        }
    }

    public DecisaoSimilaridade decide(double score) {
        if (score >= blockThreshold) return DecisaoSimilaridade.REVISAR;
        if (score >= alertThreshold) return DecisaoSimilaridade.ALERTAR;
        return DecisaoSimilaridade.PERMITIR;
    }
}
