package com.gerdau.desafio.dto.response;

import java.time.Instant;
import java.util.List;

public record CodigoServicoResponse(
        String id,
        String descricao,
        String descricaoNormalizada,
        int quantidadeCaracteres,
        String origem,
        List<String> legacyIds,
        String status,
        Instant createdAt,
        String criadoPor,
        boolean casoIsolado,
        String motivoCasoIsolado
) {
}
