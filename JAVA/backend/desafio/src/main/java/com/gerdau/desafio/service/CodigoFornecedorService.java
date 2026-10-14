package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CodigoFornecedorRequest;
import com.gerdau.desafio.dto.response.CodigoFornecedorResponse;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.CodigoFornecedorMapper;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.Origem;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import com.gerdau.desafio.repository.CodigoFornecedorRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodigoFornecedorService {

    private static final Logger log = LoggerFactory.getLogger(CodigoFornecedorService.class);
    private static final String RECURSO = "Código de Fornecedor";

    private final CodigoFornecedorRepository repository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final CodigoFornecedorMapper mapper;
    private final GeradorCodigoSequencial gerador;
    private final ConsultaPaginada consulta;

    public CodigoFornecedorService(CodigoFornecedorRepository repository,
                                   CodigoCompletoRepository codigoCompletoRepository,
                                   CodigoFornecedorMapper mapper, GeradorCodigoSequencial gerador,
                                   ConsultaPaginada consulta) {
        this.repository = repository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.mapper = mapper;
        this.gerador = gerador;
        this.consulta = consulta;
    }

    public CodigoFornecedorResponse criar(CodigoFornecedorRequest request) {
        CodigoFornecedor model = new CodigoFornecedor();
        preencher(model, request);
        model.setOrigem(Origem.SISTEMA);

        CodigoFornecedor salvo = gerador.inserirComNovoId(model, CodigoFornecedor.COLECAO,
                CodigoFornecedor.TAMANHO_ID, CodigoFornecedor::setId);
        log.info("[FORNECEDOR] Criado: id={}, codigoSAP={}", salvo.getId(), salvo.getCodigoFornecedorSAP());
        return mapper.toResponse(salvo);
    }

    public CodigoFornecedorResponse atualizar(String id, CodigoFornecedorRequest request) {
        CodigoFornecedor model = buscarEntidade(id);
        preencher(model, request);
        return mapper.toResponse(repository.save(model));
    }

    public void excluir(String id) {
        buscarEntidade(id);
        if (codigoCompletoRepository.existsByCfId(id)) {
            throw new ConflictException("O fornecedor " + id + " está em uso por códigos completos e não pode ser excluído.");
        }
        repository.deleteById(id);
        log.info("[FORNECEDOR] Excluído: id={}", id);
    }

    public List<CodigoFornecedorResponse> listar(String termo) {
        Query query = new Query();
        if (termo != null && !termo.isBlank()) {
            List<Criteria> filtros = new ArrayList<>();
            filtros.add(ConsultaPaginada.contem("nomeNormalizado", TextoUtil.normalizar(termo)));
            String digitos = termo.trim();
            if (digitos.matches("\\d{1,18}")) {
                filtros.add(Criteria.where("codigoFornecedorSAP").is(Long.parseLong(digitos)));
            }
            query.addCriteria(new Criteria().orOperator(filtros));
        }
        return consulta.listar(query, CodigoFornecedor.class).stream().map(mapper::toResponse).toList();
    }

    public CodigoFornecedorResponse buscarPorId(String id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    public CodigoFornecedor buscarEntidade(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void preencher(CodigoFornecedor model, CodigoFornecedorRequest request) {
        repository.findByCodigoFornecedorSAP(request.codigoFornecedorSAP())
                .filter(existente -> !existente.getId().equals(model.getId()))
                .ifPresent(existente -> {
                    throw new ConflictException("O código SAP " + request.codigoFornecedorSAP()
                            + " já pertence ao fornecedor " + existente.getId() + ".");
                });
        String nome = TextoUtil.limpar(request.nome());
        model.setCodigoFornecedorSAP(request.codigoFornecedorSAP());
        model.setNome(nome);
        model.setNomeNormalizado(TextoUtil.normalizar(nome));
    }
}
