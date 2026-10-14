package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.response.GrupoServicoResponse;
import com.gerdau.desafio.model.GrupoServico;
import org.springframework.stereotype.Component;

@Component
public class GrupoServicoMapper {

    public GrupoServicoResponse toResponse(GrupoServico model) {
        return new GrupoServicoResponse(model.getId(), model.getNome(), model.getNomeNormalizado(), model.getOrigem());
    }
}
