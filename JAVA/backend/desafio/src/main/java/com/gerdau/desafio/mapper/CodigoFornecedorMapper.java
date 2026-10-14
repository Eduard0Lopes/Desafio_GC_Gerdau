package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.response.CodigoFornecedorResponse;
import com.gerdau.desafio.model.CodigoFornecedor;
import org.springframework.stereotype.Component;

@Component
public class CodigoFornecedorMapper {

    public CodigoFornecedorResponse toResponse(CodigoFornecedor model) {
        return new CodigoFornecedorResponse(model.getId(), model.getCodigoFornecedorSAP(), model.getNome(),
                model.getNomeNormalizado(), model.getOrigem());
    }
}
