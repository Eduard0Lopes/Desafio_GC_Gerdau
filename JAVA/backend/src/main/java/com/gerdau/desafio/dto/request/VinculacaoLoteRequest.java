package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VinculacaoLoteRequest(

        @NotBlank(message = "Informe o Grupo de Serviço.")
        String gsId,

        @NotBlank(message = "Informe o Código de Fornecedor.")
        String cfId,

        @NotBlank(message = "Informe a Unidade de Medida.")
        String umId,

        @NotEmpty(message = "Escolha ao menos um Código de Serviço.")
        @Size(max = 300, message = "Vincule no máximo 300 códigos por vez.")
        List<ItemVinculacaoLote> itens,

        String usuarioCriacaoId

) {

    public record ItemVinculacaoLote(String csId, String texto) {
    }
}
