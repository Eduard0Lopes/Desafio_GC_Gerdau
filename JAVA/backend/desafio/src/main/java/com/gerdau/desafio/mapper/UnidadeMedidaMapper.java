package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.response.UnidadeMedidaResponse;
import com.gerdau.desafio.model.UnidadeMedida;
import org.springframework.stereotype.Component;

@Component
public class UnidadeMedidaMapper {

    public UnidadeMedidaResponse toResponse(UnidadeMedida model) {
        return new UnidadeMedidaResponse(model.getId(), model.getSigla(), model.getNome(),
                model.getNomeNormalizado(), model.getOrigem());
    }
}
