package com.gerdau.desafio.dto.request;

import java.util.List;
import java.util.Map;

public record MarcarExcecaoRequest(

        List<String> ids,

        String motivo,

        Map<String, String> motivos,

        Boolean limpar
) {
}
