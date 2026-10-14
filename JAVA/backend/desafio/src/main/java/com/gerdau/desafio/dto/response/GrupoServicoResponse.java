package com.gerdau.desafio.dto.response;

import com.gerdau.desafio.model.Origem;

public record GrupoServicoResponse(String id, String nome, String nomeNormalizado, Origem origem) {
}
