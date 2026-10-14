package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CodigoFornecedorRequest(

        @NotNull(message = "O código SAP do fornecedor é obrigatório.")
        @Positive(message = "O código SAP do fornecedor deve ser positivo.")
        Long codigoFornecedorSAP,

        @NotBlank(message = "O nome do fornecedor é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome
) {
}
