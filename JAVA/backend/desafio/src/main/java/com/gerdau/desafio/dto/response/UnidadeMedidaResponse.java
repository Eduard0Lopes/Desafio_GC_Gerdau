package com.gerdau.desafio.dto.response;

import com.gerdau.desafio.model.Origem;

public record UnidadeMedidaResponse(String id, String sigla, String nome, String nomeNormalizado, Origem origem) {
}
