package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.UnidadeMedidaRequest;
import com.gerdau.desafio.dto.response.UnidadeMedidaResponse;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.UnidadeMedidaMapper;
import com.gerdau.desafio.model.Origem;
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import com.gerdau.desafio.repository.UnidadeMedidaRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UnidadeMedidaService {

    private static final Logger log = LoggerFactory.getLogger(UnidadeMedidaService.class);
    private static final String RECURSO = "Unidade de Medida";

    private final UnidadeMedidaRepository repository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final UnidadeMedidaMapper mapper;
    private final GeradorCodigoSequencial gerador;
    private final ConsultaPaginada consulta;

    public UnidadeMedidaService(UnidadeMedidaRepository repository, CodigoCompletoRepository codigoCompletoRepository,
                                UnidadeMedidaMapper mapper, GeradorCodigoSequencial gerador, ConsultaPaginada consulta) {
        this.repository = repository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.mapper = mapper;
        this.gerador = gerador;
        this.consulta = consulta;
    }

    public UnidadeMedidaResponse criar(UnidadeMedidaRequest request) {
        UnidadeMedida model = new UnidadeMedida();
        preencher(model, request);
        model.setOrigem(Origem.SISTEMA);

        UnidadeMedida salvo = gerador.inserirComNovoId(model, UnidadeMedida.COLECAO, UnidadeMedida.TAMANHO_ID,
                UnidadeMedida::setId);
        log.info("[UNIDADE] Criada: id={}, sigla='{}'", salvo.getId(), salvo.getSigla());
        return mapper.toResponse(salvo);
    }

    public UnidadeMedidaResponse atualizar(String id, UnidadeMedidaRequest request) {
        UnidadeMedida model = buscarEntidade(id);
        preencher(model, request);
        return mapper.toResponse(repository.save(model));
    }

    public void excluir(String id) {
        buscarEntidade(id);
        if (codigoCompletoRepository.existsByUmId(id)) {
            throw new ConflictException("A unidade " + id + " está em uso por códigos completos e não pode ser excluída.");
        }
        repository.deleteById(id);
        log.info("[UNIDADE] Excluída: id={}", id);
    }

    public List<UnidadeMedidaResponse> listar(String termo) {
        Query query = new Query();
        if (termo != null && !termo.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    ConsultaPaginada.contem("sigla", termo.trim()),
                    ConsultaPaginada.contem("nomeNormalizado", TextoUtil.normalizar(termo))));
        }
        return consulta.listar(query, UnidadeMedida.class).stream().map(mapper::toResponse).toList();
    }

    public UnidadeMedidaResponse buscarPorId(String id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    public UnidadeMedida buscarEntidade(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void preencher(UnidadeMedida model, UnidadeMedidaRequest request) {
        String sigla = request.sigla().trim().toUpperCase(Locale.ROOT);
        repository.findBySigla(sigla)
                .filter(existente -> !existente.getId().equals(model.getId()))
                .ifPresent(existente -> {
                    throw new ConflictException("Já existe a unidade " + existente.getId() + " com a sigla '" + sigla + "'.");
                });
        String nome = TextoUtil.limpar(request.nome());
        model.setSigla(sigla);
        model.setNome(nome);
        model.setNomeNormalizado(TextoUtil.normalizar(nome));
    }
}
