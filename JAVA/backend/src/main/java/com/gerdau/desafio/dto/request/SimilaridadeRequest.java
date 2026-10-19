package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimilaridadeRequest(

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 200, message = "A descrição de consulta não pode exceder 200 caracteres.")
        String descricao,

        @Min(value = 1, message = "O limite mínimo é 1.")
        @Max(value = 20, message = "O limite máximo é 20.")
        Integer limite,

        String tipo
) {
}
