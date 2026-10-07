package br.com.gerdau.servicos.api.domain;

public enum DecisaoSimilaridade {
    /** Sem similar relevante: segue o fluxo normal. */
    PERMITIR,
    /** Similaridade intermediária: alerta; o usuário reutiliza ou justifica. */
    ALERTAR,
    /** Similaridade alta: bloqueia; o usuário reutiliza, corrige ou envia para a Governança. */
    REVISAR
}
