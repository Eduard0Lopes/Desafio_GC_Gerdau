package com.gerdau.desafio.dto.response;

import java.util.List;

public record LoteSimilaridadeResponse(

        String tipo,
        int total,
        int disponiveis,
        int aCriar,
        int bloqueadas,
        List<ItemLoteResponse> itens

) {
}
