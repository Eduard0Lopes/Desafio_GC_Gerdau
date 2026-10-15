package com.gerdau.desafio.dto.request;

/**
 * Prévia de código completo: para cada componente, informa o id oficial de um item
 * existente OU os dados de um novo item (fluxo "Cadastrar Novo Item").
 */
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
