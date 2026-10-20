package com.gerdau.desafio.dto.response;

import java.util.List;

/**
 * Resumo da criação em massa — é o aviso da tela de informação:
 * "há X disponíveis, porém Y precisam ser criados" (+ Z bloqueadas).
 */
public record LoteSimilaridadeResponse(

        String tipo,
        int total,
        int disponiveis,
        int aCriar,
        int bloqueadas,
        List<ItemLoteResponse> itens

) {
}
