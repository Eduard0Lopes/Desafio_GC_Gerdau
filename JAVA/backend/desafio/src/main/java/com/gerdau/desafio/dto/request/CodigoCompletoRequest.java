package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CodigoCompletoRequest(

        @NotBlank(message = "O idCompleto é obrigatório.")
        String idCompleto,
        String gsId,
        String cfId,
        String csId,
        String umId,
        String descricaoSnapshot,
        String origem,
        String cenarioTeste
) {
}