package com.gerdau.desafio.dto.response;

import java.util.List;

public record LoteVinculacaoResponse(

        int total,
        int reutilizados,
        int criados,
        int vinculados,
        int aguardandoValidacao,
        List<ItemVinculacaoResponse> itens

) {
}
