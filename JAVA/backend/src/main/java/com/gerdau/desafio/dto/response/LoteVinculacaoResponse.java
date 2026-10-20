package com.gerdau.desafio.dto.response;

import java.util.List;

/**
 * Resumo da vinculação em massa: quantos códigos já estavam disponíveis
 * (reutilizados), quantos precisaram ser criados, quantos vínculos saíram ativos
 * e quantos foram para a validação da Governança.
 */
public record LoteVinculacaoResponse(

        int total,
        int disponiveis,
        int criados,
        int vinculados,
        int aguardandoValidacao,
        int bloqueadas,
        int falhas,
        List<ItemVinculacaoResponse> itens

) {
}
