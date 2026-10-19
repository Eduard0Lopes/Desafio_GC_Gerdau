package com.gerdau.desafio.dto.response;

import java.time.Instant;

public record UnidadeMedidaResponse(String id, String sigla, String nome, String nomeNormalizado, String origem,
                                    String status, Instant createdAt, String criadoPor) {
}
