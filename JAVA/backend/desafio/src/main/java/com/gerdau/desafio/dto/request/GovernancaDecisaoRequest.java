package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Decisão humana sobre um novo cadastro pendente de validação. */
public record GovernancaDecisaoRequest(

        @NotBlank(message = "O responsável pela decisão é obrigatório.")
        String aprovadoPor,

        String observacao
) {
}
