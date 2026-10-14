package com.gerdau.desafio.dto.response;

import com.gerdau.desafio.model.Origem;

public record CodigoFornecedorResponse(String id, Long codigoFornecedorSAP, String nome, String nomeNormalizado,
                                       Origem origem) {
}
