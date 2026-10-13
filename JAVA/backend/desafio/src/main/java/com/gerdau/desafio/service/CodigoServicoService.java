package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CodigoServicoRequest;
import com.gerdau.desafio.dto.response.CodigoServicoResponse;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.CodigoServicoMapper;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodigoServicoService {

    private static final Logger log = LoggerFactory.getLogger(CodigoServicoService.class);

    private final CodigoServicoRepository repository;
    private final CodigoServicoMapper mapper;

    public CodigoServicoService(CodigoServicoRepository repository, CodigoServicoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public CodigoServicoResponse criar(CodigoServicoRequest request) {
        CodigoServico model = mapper.toModel(request);

        model.setDescricaoNormalizada(TextoUtil.normalizar(request.descricao()));
        model.setQuantidadeCaracteres(request.descricao().length());

        if (model.getOrigem() == null || model.getOrigem().isBlank()) {
            model.setOrigem("SISTEMA");
        }

        if (model.getCodigoOriginal() == null || model.getCodigoOriginal().isBlank()) {
            model.setCodigoOriginal(gerarProximoCodigo());
        }

        CodigoServico salvo = repository.save(model);

        log.info("[CODIGO] Criado: id={}, codigoOriginal={}, chars={}",
                salvo.getId(), salvo.getCodigoOriginal(), salvo.getQuantidadeCaracteres());

        return mapper.toResponse(salvo);
    }

    public List<CodigoServicoResponse> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CodigoServicoResponse buscarPorId(String id) {
        CodigoServico model = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código de Serviço", id));
        return mapper.toResponse(model);
    }

    private String gerarProximoCodigo() {
        long total = repository.count();
        return String.format("%06d", total + 1);
    }
}