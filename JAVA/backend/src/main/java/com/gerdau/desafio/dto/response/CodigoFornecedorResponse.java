package com.gerdau.desafio.dto.response;

import java.time.Instant;

public record CodigoFornecedorResponse(
        String id,
        Long codigoFornecedorSAP,
        String nome,
        String nomeNormalizado,
        String origem,
        String status,
        Instant createdAt,
        String criadoPor
) {
}
