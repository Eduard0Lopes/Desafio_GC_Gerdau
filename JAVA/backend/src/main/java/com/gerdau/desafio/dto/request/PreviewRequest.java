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

    public static PreviewRequest deComposicao(String gsId, String cfId, String csId, String umId) {
        return new PreviewRequest(gsId, null, cfId, null, null, csId, null, umId, null, null);
    }
}
