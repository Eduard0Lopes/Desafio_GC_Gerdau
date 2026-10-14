package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.GrupoServicoRequest;
import com.gerdau.desafio.dto.response.GrupoServicoResponse;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.GrupoServicoMapper;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.Origem;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import com.gerdau.desafio.repository.GrupoServicoRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrupoServicoService {

    private static final Logger log = LoggerFactory.getLogger(GrupoServicoService.class);
    private static final String RECURSO = "Grupo de Serviço";

    private final GrupoServicoRepository repository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final GrupoServicoMapper mapper;
    private final GeradorCodigoSequencial gerador;
    private final ConsultaPaginada consulta;

    public GrupoServicoService(GrupoServicoRepository repository, CodigoCompletoRepository codigoCompletoRepository,
                               GrupoServicoMapper mapper, GeradorCodigoSequencial gerador, ConsultaPaginada consulta) {
        this.repository = repository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.mapper = mapper;
        this.gerador = gerador;
        this.consulta = consulta;
    }

    public GrupoServicoResponse criar(GrupoServicoRequest request) {
        GrupoServico model = new GrupoServico();
        preencher(model, request);
        model.setOrigem(Origem.SISTEMA);

        GrupoServico salvo = gerador.inserirComNovoId(model, GrupoServico.COLECAO, GrupoServico.TAMANHO_ID,
                GrupoServico::setId);
        log.info("[GRUPO] Criado: id={}, nome='{}'", salvo.getId(), salvo.getNome());
        return mapper.toResponse(salvo);
    }

    public GrupoServicoResponse atualizar(String id, GrupoServicoRequest request) {
        GrupoServico model = buscarEntidade(id);
        preencher(model, request);
        return mapper.toResponse(repository.save(model));
    }

    public void excluir(String id) {
        buscarEntidade(id);
        if (codigoCompletoRepository.existsByGsId(id)) {
            throw new ConflictException("O grupo " + id + " está em uso por códigos completos e não pode ser excluído.");
        }
        repository.deleteById(id);
        log.info("[GRUPO] Excluído: id={}", id);
    }

    public List<GrupoServicoResponse> listar(String termo) {
        Query query = new Query();
        if (termo != null && !termo.isBlank()) {
            query.addCriteria(ConsultaPaginada.contem("nomeNormalizado", TextoUtil.normalizar(termo)));
        }
        return consulta.listar(query, GrupoServico.class).stream().map(mapper::toResponse).toList();
    }

    public GrupoServicoResponse buscarPorId(String id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    public GrupoServico buscarEntidade(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void preencher(GrupoServico model, GrupoServicoRequest request) {
        String nome = TextoUtil.limpar(request.nome());
        String normalizado = TextoUtil.normalizar(nome);
        repository.findByNomeNormalizado(normalizado)
                .filter(existente -> !existente.getId().equals(model.getId()))
                .ifPresent(existente -> {
                    throw new ConflictException("Já existe o grupo " + existente.getId() + " com o nome '" + existente.getNome() + "'.");
                });
        model.setNome(nome);
        model.setNomeNormalizado(normalizado);
    }
}
