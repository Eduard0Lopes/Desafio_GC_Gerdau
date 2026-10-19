package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RevisaoMatrizRequest(

        @NotBlank(message = "O responsável pela revisão é obrigatório.")
        String revisadoPor,

        String acao,

        String observacao
) {
}
