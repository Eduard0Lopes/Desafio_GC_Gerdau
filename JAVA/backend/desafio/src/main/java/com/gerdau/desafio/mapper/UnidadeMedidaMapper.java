package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.request.UnidadeMedidaRequest;
import com.gerdau.desafio.dto.response.UnidadeMedidaResponse;
import com.gerdau.desafio.model.UnidadeMedida;
import org.springframework.stereotype.Component;

@Component
public class UnidadeMedidaMapper {

    public UnidadeMedida toModel(UnidadeMedidaRequest request) {
        UnidadeMedida model = new UnidadeMedida();
        model.setSigla(request.sigla());
        model.setNome(request.nome());
        model.setCodigoOriginal(request.codigoOriginal());
        return model;
    }

    public UnidadeMedidaResponse toResponse(UnidadeMedida model) {
        return new UnidadeMedidaResponse(
                model.getId(),
                model.getSigla(),
                model.getNome(),
                model.getNomeNormalizado(),
                model.getCodigoOriginal(),
                model.getOrigem()
        );
    }
}