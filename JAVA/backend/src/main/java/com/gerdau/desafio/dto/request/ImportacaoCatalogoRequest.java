package com.gerdau.desafio.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ImportacaoCatalogoRequest(

        @NotBlank(message = "O tipo de componente e obrigatorio (GS, CF, CS ou UM).")
        String tipo,

        @Valid
        List<ItemImportacao> itens) {

    public record ItemImportacao(String id, String nome, String descricao, String sigla, String codigoSap) {
    }
}
