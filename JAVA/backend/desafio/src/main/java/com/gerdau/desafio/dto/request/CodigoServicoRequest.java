package com.gerdau.desafio.dto.request;

import com.gerdau.desafio.util.TextoUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodigoServicoRequest(

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = TextoUtil.LIMITE_SAP, message = "A descrição não pode exceder 40 caracteres (regra SAP).")
        String descricao,

        @Size(max = 500, message = "A justificativa deve ter no máximo 500 caracteres.")
        String justificativa
) {
}
