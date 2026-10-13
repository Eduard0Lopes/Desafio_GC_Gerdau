package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.UnidadeMedidaRequest;
import com.gerdau.desafio.dto.response.UnidadeMedidaResponse;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.UnidadeMedidaMapper;
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.UnidadeMedidaRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnidadeMedidaService {

    private static final Logger log = LoggerFactory.getLogger(UnidadeMedidaService.class);

    private final UnidadeMedidaRepository repository;
    private final UnidadeMedidaMapper mapper;

    public UnidadeMedidaService(UnidadeMedidaRepository repository, UnidadeMedidaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public UnidadeMedidaResponse criar(UnidadeMedidaRequest request) {
        UnidadeMedida model = mapper.toModel(request);

        model.setNomeNormalizado(TextoUtil.normalizar(request.nome()));

        if (model.getOrigem() == null || model.getOrigem().isBlank()) {
            model.setOrigem("SISTEMA");
        }

        if (model.getCodigoOriginal() == null || model.getCodigoOriginal().isBlank()) {
            model.setCodigoOriginal(gerarProximoCodigo());
        }

        UnidadeMedida salvo = repository.save(model);

        log.info("[UNIDADE] Criada: id={}, sigla='{}', nome='{}'",
                salvo.getId(), salvo.getSigla(), salvo.getNome());

        return mapper.toResponse(salvo);
    }

    public List<UnidadeMedidaResponse> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public UnidadeMedidaResponse buscarPorId(String id) {
        UnidadeMedida model = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de Medida", id));
        return mapper.toResponse(model);
    }

    private String gerarProximoCodigo() {
        long total = repository.count();
        return String.format("%06d", total + 1);
    }
}