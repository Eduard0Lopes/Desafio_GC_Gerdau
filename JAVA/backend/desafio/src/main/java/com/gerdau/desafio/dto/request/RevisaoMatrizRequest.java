package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Revisão humana de um registro histórico da matriz (seção 9.6):
 * acao CONSOLIDAR (mantém o código canônico) | MANTER (exceção justificada).
 */
public record RevisaoMatrizRequest(

        @NotBlank(message = "O responsável pela revisão é obrigatório.")
        String revisadoPor,

        String acao,

        String observacao
) {
}
