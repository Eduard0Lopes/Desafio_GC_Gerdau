package com.gerdau.desafio.dto.response;

import com.gerdau.desafio.model.Origem;

import java.util.List;

public record CodigoServicoResponse(
        String id,
        String descricao,
        String descricaoNormalizada,
        int quantidadeCaracteres,
        Origem origem,
        String justificativa,
        List<String> legacyIds
) {
}
