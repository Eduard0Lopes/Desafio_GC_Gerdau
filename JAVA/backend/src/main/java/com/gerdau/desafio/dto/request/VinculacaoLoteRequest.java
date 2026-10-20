package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Vinculação em massa: Grupo de Serviço, Código de Fornecedor e Unidade de Medida
 * fixos para toda a lista e vários Códigos de Serviço escolhidos de uma vez.
 */
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

    /**
     * Um item do lote: {@code csId} quando o código já existe e será reutilizado;
     * senão {@code texto} é um código novo — o servidor aplica as regras
     * (similaridade, limite SAP) e decide entre criar ou enviar para a Governança.
     */
    public record ItemVinculacaoLote(String csId, String texto) {
    }
}
