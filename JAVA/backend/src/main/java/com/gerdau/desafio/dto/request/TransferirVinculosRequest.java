package com.gerdau.desafio.dto.request;

public record TransferirVinculosRequest(

        String tipo,

        String de,

        String para,

        Boolean excluirOrigem
) {
}
