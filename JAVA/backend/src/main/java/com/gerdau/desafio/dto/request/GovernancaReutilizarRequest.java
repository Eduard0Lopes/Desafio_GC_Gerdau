package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GovernancaReutilizarRequest(

        @NotBlank(message = "Informe o item similar escolhido.")
        String escolhidoId,

        String escolhidoTipo,

        String aprovadoPor,

        String observacao

) {
}
