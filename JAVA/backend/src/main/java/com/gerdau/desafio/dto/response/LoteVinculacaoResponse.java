package com.gerdau.desafio.dto.response;

import java.util.List;

/**
 * Resumo da vinculação em massa: quantos códigos foram reutilizados, quantos
 * criados, quantos vínculos saíram ativos e quantos foram para a validação da
 * Governança. As pendências (bloqueadas e falhas) ficam em {@code itens}.
 */
public record LoteVinculacaoResponse(

        int total,
        int reutilizados,
        int criados,
        int vinculados,
        int aguardandoValidacao,
        List<ItemVinculacaoResponse> itens

) {
}
