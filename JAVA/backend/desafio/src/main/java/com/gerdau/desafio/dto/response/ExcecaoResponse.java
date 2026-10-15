package com.gerdau.desafio.dto.response;

import com.gerdau.desafio.model.RegistroSimilar;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Registro de exceção/auditoria (seção 12.3). */
public record ExcecaoResponse(
        String id,
        String solicitante,
        Instant dataHora,
        Map<String, Object> payloadOriginal,
        List<RegistroSimilar> similaresEncontrados,
        String justificativa,
        String decisaoTomada,
        String observacao,
        String codigoCompletoId
) {
}
