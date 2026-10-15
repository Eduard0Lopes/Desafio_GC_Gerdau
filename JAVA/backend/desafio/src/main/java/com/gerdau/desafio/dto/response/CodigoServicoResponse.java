package com.gerdau.desafio.dto.response;

import java.util.List;

public record CodigoServicoResponse(
        String id,
        String descricao,
        String descricaoNormalizada,
        int quantidadeCaracteres,
        String origem,
        List<String> legacyIds
) {
}
