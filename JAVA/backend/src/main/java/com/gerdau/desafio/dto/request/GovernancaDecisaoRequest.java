package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GovernancaDecisaoRequest(

        @NotBlank(message = "O responsável pela decisão é obrigatório.")
        String aprovadoPor,

        String observacao
) {
}
