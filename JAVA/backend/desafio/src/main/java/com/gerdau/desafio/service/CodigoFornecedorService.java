package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CodigoFornecedorRequest;
import com.gerdau.desafio.dto.response.CodigoFornecedorResponse;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.CodigoFornecedorMapper;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.repository.CodigoFornecedorRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodigoFornecedorService {

    private static final Logger log = LoggerFactory.getLogger(CodigoFornecedorService.class);

    private final CodigoFornecedorRepository repository;
    private final CodigoFornecedorMapper mapper;

    public CodigoFornecedorService(CodigoFornecedorRepository repository, CodigoFornecedorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public CodigoFornecedorResponse criar(CodigoFornecedorRequest request) {
        CodigoFornecedor model = mapper.toModel(request);

        model.setNomeNormalizado(TextoUtil.normalizar(request.nomeNormalizado()));

        if (model.getOrigem() == null || model.getOrigem().isBlank()) {
            model.setOrigem("SISTEMA");
        }

        CodigoFornecedor salvo = repository.save(model);

        log.info("[FORNECEDOR] Criado: id={}, codigoOriginal={}",
                salvo.getId(), salvo.getCodigoOriginal());

        return mapper.toResponse(salvo);
    }

    public List<CodigoFornecedorResponse> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CodigoFornecedorResponse buscarPorId(String id) {
        CodigoFornecedor model = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código de Fornecedor", id));
        return mapper.toResponse(model);
    }
}