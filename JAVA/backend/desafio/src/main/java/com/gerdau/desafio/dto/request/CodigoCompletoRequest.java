package com.gerdau.desafio.dto.request;

import com.gerdau.desafio.util.TextoUtil;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Informe {@code csId} para reutilizar um código de serviço existente
 * ou {@code descricaoServico} para cadastrar um novo no mesmo passo.
 */
public record CodigoCompletoRequest(

        @NotBlank(message = "O grupo de serviço (gsId) é obrigatório.")
        String gsId,

        @NotBlank(message = "O fornecedor (cfId) é obrigatório.")
        String cfId,

        @NotBlank(message = "A unidade de medida (umId) é obrigatória.")
        String umId,

        String csId,

        @Size(max = TextoUtil.LIMITE_SAP, message = "A descrição não pode exceder 40 caracteres (regra SAP).")
        String descricaoServico,

        @Size(max = 500, message = "A justificativa deve ter no máximo 500 caracteres.")
        String justificativa
) {

    @AssertTrue(message = "Informe exatamente um entre csId (serviço existente) e descricaoServico (novo serviço).")
    public boolean isServicoInformadoUmaVez() {
        return isPreenchido(csId) ^ isPreenchido(descricaoServico);
    }

    private static boolean isPreenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
