package com.gerdau.desafio.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Decisão da Governança de reutilizar um código existente para um pedido
 * pendente — é o mesmo gesto do radio do bloqueio de duplicidade, mas feito
 * por quem aprova, sobre os matches que o solicitante não escolheu.
 */
public record GovernancaReutilizarRequest(

        @NotBlank(message = "Informe o item similar escolhido.")
        String escolhidoId,

        /** CS, GS, CF ou UM — quando ausente, vale o tipo registrado no snapshot do match. */
        String escolhidoTipo,

        String aprovadoPor,

        String observacao

) {
}
