package com.gerdau.desafio.dto.response;

import com.gerdau.desafio.model.Origem;

import java.time.Instant;
import java.util.List;

public record CodigoCompletoResponse(
        String id,
        String gsId,
        String grupoNome,
        String cfId,
        Long fornecedorCodigoSAP,
        String fornecedorNome,
        String csId,
        String servicoDescricao,
        String umId,
        String unidadeSigla,
        String unidadeNome,
        String descricaoSnapshot,
        Origem origem,
        Instant createdAt,
        List<String> legacyIds
) {
}
