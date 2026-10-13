package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.request.CodigoFornecedorRequest;
import com.gerdau.desafio.dto.response.CodigoFornecedorResponse;
import com.gerdau.desafio.model.CodigoFornecedor;
import org.springframework.stereotype.Component;

@Component
public class CodigoFornecedorMapper {

    public CodigoFornecedor toModel(CodigoFornecedorRequest request) {
        CodigoFornecedor model = new CodigoFornecedor();
        model.setCodigoOriginal(request.codigoOriginal());
        model.setNomeNormalizado(request.nomeNormalizado());
        return model;
    }

    public CodigoFornecedorResponse toResponse(CodigoFornecedor model) {
        return new CodigoFornecedorResponse(
                model.getId(),
                model.getCodigoOriginal(),
                model.getNomeNormalizado(),
                model.getOrigem()
        );
    }
}