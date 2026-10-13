package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.request.GrupoServicoRequest;
import com.gerdau.desafio.dto.response.GrupoServicoResponse;
import com.gerdau.desafio.model.GrupoServico;
import org.springframework.stereotype.Component;

@Component
public class GrupoServicoMapper {

    public GrupoServico toModel(GrupoServicoRequest request) {
        GrupoServico model = new GrupoServico();
        model.setNome(request.nome());
        model.setCodigoOriginal(request.codigoOriginal());
        return model;
    }

    public GrupoServicoResponse toResponse(GrupoServico model) {
        return new GrupoServicoResponse(
                model.getId(),
                model.getNome(),
                model.getNomeNormalizado(),
                model.getCodigoOriginal(),
                model.getOrigem()
        );
    }
}