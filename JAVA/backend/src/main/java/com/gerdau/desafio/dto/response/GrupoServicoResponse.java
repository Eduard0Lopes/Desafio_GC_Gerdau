package com.gerdau.desafio.dto.response;

import java.time.Instant;

public record GrupoServicoResponse(String id, String nome, String nomeNormalizado, String origem, String status,
                                   Instant createdAt, String criadoPor) {
}
