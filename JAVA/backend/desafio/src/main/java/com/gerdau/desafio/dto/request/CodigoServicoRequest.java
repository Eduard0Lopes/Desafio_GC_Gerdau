package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodigoServicoRequest(

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 40, message = "A descrição não pode exceder 40 caracteres (regra SAP).")
        String descricao,

        String codigoOriginal,

        String grupoServicoId,
        String unidadeMedidaId
) {
}