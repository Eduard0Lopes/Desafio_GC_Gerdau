package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CodigoFornecedorRequest(

        @NotBlank(message = "O código original é obrigatório.")
        String codigoOriginal,

        @NotBlank(message = "O nome normalizado é obrigatório.")
        String nomeNormalizado
) {
}