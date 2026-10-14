package com.gerdau.desafio.service.similaridade;

public enum StatusSimilaridade {
    DUPLICIDADE("Provável duplicidade - Bloquear ou exigir justificativa"),
    ALERTA("Alerta de similaridade - Encaminhar para decisão"),
    ORIGINAL("Permitir continuidade - Novo registro com validação");

    private final String acaoRecomendada;

    StatusSimilaridade(String acaoRecomendada) {
        this.acaoRecomendada = acaoRecomendada;
    }

    public String getAcaoRecomendada() {
        return acaoRecomendada;
    }
}
