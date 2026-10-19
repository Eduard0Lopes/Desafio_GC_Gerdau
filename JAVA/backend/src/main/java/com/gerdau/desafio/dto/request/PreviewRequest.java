package com.gerdau.desafio.dto.request;

public record PreviewRequest(

        String gsId,
        String gsNovoNome,

        String cfId,
        String cfNovoNome,
        String cfNovoCodigoSap,

        String csId,
        String csNovaDescricao,

        String umId,
        String umNovaSigla,
        String umNovoNome
) {
}
