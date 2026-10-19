package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.Size;

public record CriarCodigoCompletoRequest(

        String gsId,
        String gsNovoNome,

        String cfId,
        String cfNovoNome,
        String cfNovoCodigoSap,

        String csId,

        @Size(max = 200, message = "A descrição nova não pode exceder 200 caracteres.")
        String csNovaDescricao,

        String umId,
        String umNovaSigla,
        String umNovoNome,

        @Size(max = 500, message = "A justificativa não pode exceder 500 caracteres.")
        String justificativa,

        String acao,

        String usuarioCriacaoId
) {
}
