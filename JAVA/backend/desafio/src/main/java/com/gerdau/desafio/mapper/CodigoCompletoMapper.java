package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.UnidadeMedida;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CodigoCompletoMapper {

    public CodigoCompletoResponse toResponse(CodigoCompleto model, GrupoServico grupo, CodigoFornecedor fornecedor,
                                             CodigoServico servico, UnidadeMedida unidade) {
        Optional<GrupoServico> g = Optional.ofNullable(grupo);
        Optional<CodigoFornecedor> f = Optional.ofNullable(fornecedor);
        Optional<UnidadeMedida> u = Optional.ofNullable(unidade);
        return new CodigoCompletoResponse(
                model.getId(),
                model.getGsId(),
                g.map(GrupoServico::getNome).orElse(null),
                model.getCfId(),
                f.map(CodigoFornecedor::getCodigoFornecedorSAP).orElse(null),
                f.map(CodigoFornecedor::getNome).orElse(null),
                model.getCsId(),
                Optional.ofNullable(servico).map(CodigoServico::getDescricao).orElse(null),
                model.getUmId(),
                u.map(UnidadeMedida::getSigla).orElse(null),
                u.map(UnidadeMedida::getNome).orElse(null),
                model.getDescricaoSnapshot(),
                model.getOrigem(),
                model.getCreatedAt(),
                model.getLegacyIds() != null ? model.getLegacyIds() : List.of()
        );
    }
}
