package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Criação de código completo com governança de entrada (seções 10.2/10.3 e 12).
 * acao: CRIAR (exige justificativa quando houver alerta) | ENVIAR_VALIDACAO.
 */
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
