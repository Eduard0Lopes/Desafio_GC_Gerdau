package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.GrupoServicoRequest;
import com.gerdau.desafio.dto.response.GrupoServicoResponse;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.GrupoServicoMapper;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.repository.GrupoServicoRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrupoServicoService {

    private static final Logger log = LoggerFactory.getLogger(GrupoServicoService.class);

    private final GrupoServicoRepository repository;
    private final GrupoServicoMapper mapper;

    public GrupoServicoService(GrupoServicoRepository repository, GrupoServicoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public GrupoServicoResponse criar(GrupoServicoRequest request) {
        GrupoServico model = mapper.toModel(request);

        model.setNomeNormalizado(TextoUtil.normalizar(request.nome()));

        if (model.getOrigem() == null || model.getOrigem().isBlank()) {
            model.setOrigem("SISTEMA");
        }

        if (model.getCodigoOriginal() == null || model.getCodigoOriginal().isBlank()) {
            model.setCodigoOriginal(gerarProximoCodigo());
        }

        GrupoServico salvo = repository.save(model);

        log.info("[GRUPO] Criado: id={}, nome='{}'", salvo.getId(), salvo.getNome());

        return mapper.toResponse(salvo);
    }

    public List<GrupoServicoResponse> listarTodos() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    public GrupoServicoResponse buscarPorId(String id) {
        GrupoServico model = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de Serviço", id));
        return mapper.toResponse(model);
    }

    private String gerarProximoCodigo() {
        long total = repository.count();
        return String.format("%06d", total + 1);
    }
}