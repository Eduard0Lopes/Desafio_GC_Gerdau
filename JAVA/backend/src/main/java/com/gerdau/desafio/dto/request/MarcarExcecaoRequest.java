package com.gerdau.desafio.dto.request;

import java.util.List;
import java.util.Map;

public record MarcarExcecaoRequest(

        List<String> ids,

        String motivo,

        /** motivo específico por id (sobrepõe o motivo geral) */
        Map<String, String> motivos,

        /** true = remove a marcação (!) dos itens listados */
        Boolean limpar
) {
}
