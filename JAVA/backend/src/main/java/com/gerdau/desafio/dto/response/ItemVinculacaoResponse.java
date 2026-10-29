package com.gerdau.desafio.dto.response;

public record ItemVinculacaoResponse(

        int linha,
        String texto,
        String classificacao,
        String decisao,
        String motivo,
        String csId,
        String idCompleto,
        boolean requisicaoRepetida

) {
}
