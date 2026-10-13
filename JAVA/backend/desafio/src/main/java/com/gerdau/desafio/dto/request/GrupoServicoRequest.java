package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrupoServicoRequest(

        @NotBlank(message = "O nome do grupo de serviço é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
        String nome,

        String codigoOriginal
) {
}