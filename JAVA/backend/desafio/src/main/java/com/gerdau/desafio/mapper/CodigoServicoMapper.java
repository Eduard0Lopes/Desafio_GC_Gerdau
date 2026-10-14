package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.request.CodigoServicoRequest;
import com.gerdau.desafio.dto.response.CodigoServicoResponse;
import com.gerdau.desafio.model.CodigoServico;
import org.springframework.stereotype.Component;

@Component
public class CodigoServicoMapper {
    public CodigoServico toModel(CodigoServicoRequest request) {
        CodigoServico model = new CodigoServico();
        model.setDescricao(request.descricao().trim());
        model.setCodigoOriginal(request.codigoOriginal());
        model.setGrupoServicoId(request.grupoServicoId());
        model.setUnidadeMedidaId(request.unidadeMedidaId());
        return model;
    }

    public CodigoServicoResponse toResponse(CodigoServico model) {
        return new CodigoServicoResponse(model.getId(), model.getCodigoOriginal(),
                model.getDescricao(), model.getDescricaoNormalizada(),
                model.getQuantidadeCaracteres(), model.getOrigem(),
                model.getGrupoServicoId(), model.getUnidadeMedidaId());
    }
}
