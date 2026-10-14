package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.response.CodigoServicoResponse;
import com.gerdau.desafio.model.CodigoServico;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CodigoServicoMapper {

    public CodigoServicoResponse toResponse(CodigoServico model) {
        return new CodigoServicoResponse(
                model.getId(),
                model.getDescricao(),
                model.getDescricaoNormalizada(),
                model.getQuantidadeCaracteres(),
                model.getOrigem(),
                model.getJustificativa(),
                model.getLegacyIds() != null ? model.getLegacyIds() : List.of()
        );
    }
}
