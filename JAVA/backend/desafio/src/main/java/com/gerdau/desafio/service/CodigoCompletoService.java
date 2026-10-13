package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CodigoCompletoRequest;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.CodigoCompletoMapper;
import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodigoCompletoService {

    private static final Logger log = LoggerFactory.getLogger(CodigoCompletoService.class);

    private final CodigoCompletoRepository repository;
    private final CodigoCompletoMapper mapper;

    public CodigoCompletoService(CodigoCompletoRepository repository, CodigoCompletoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public CodigoCompletoResponse criar(CodigoCompletoRequest request) {
        if (repository.existsById(request.idCompleto())) {
            throw new BusinessException("Já existe um código completo com o ID: " + request.idCompleto());
        }

        CodigoCompleto model = mapper.toModel(request);

        if (model.getOrigem() == null || model.getOrigem().isBlank()) {
            model.setOrigem("MIGRACAO");
        }

        CodigoCompleto salvo = repository.save(model);

        log.info("[CODIGO COMPLETO] Criado: id={}, gsId={}, cfId={}",
                salvo.getId(), salvo.getGsId(), salvo.getCfId());

        return mapper.toResponse(salvo);
    }

    public List<CodigoCompletoResponse> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CodigoCompletoResponse buscarPorId(String id) {
        CodigoCompleto model = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código Completo", id));
        return mapper.toResponse(model);
    }
}