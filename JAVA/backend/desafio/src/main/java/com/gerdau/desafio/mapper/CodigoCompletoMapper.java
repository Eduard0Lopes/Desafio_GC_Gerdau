package com.gerdau.desafio.mapper;

import com.gerdau.desafio.dto.request.CodigoCompletoRequest;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.model.CodigoCompleto;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class CodigoCompletoMapper {

    public CodigoCompleto toModel(CodigoCompletoRequest request) {
        CodigoCompleto model = new CodigoCompleto();
        model.setId(request.idCompleto());
        model.setIdCompleto(request.idCompleto());
        model.setGsId(request.gsId());
        model.setCfId(request.cfId());
        model.setCsId(request.csId());
        model.setUmId(request.umId());
        model.setDescricaoSnapshot(request.descricaoSnapshot());
        model.setOrigem(request.origem());
        model.setCenarioTeste(request.cenarioTeste());
        return model;
    }

    public CodigoCompletoResponse toResponse(CodigoCompleto model) {
        return new CodigoCompletoResponse(
                model.getId(),
                model.getIdCompleto(),
                model.getGsId(),
                model.getCfId(),
                model.getCsId(),
                model.getUmId(),
                model.getDescricaoSnapshot(),
                model.getOrigem(),
                model.getLegacyIds() != null ? model.getLegacyIds() : Collections.emptyList()
        );
    }
}