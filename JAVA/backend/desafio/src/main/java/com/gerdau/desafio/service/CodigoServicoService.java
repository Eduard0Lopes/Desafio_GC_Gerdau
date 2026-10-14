package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CodigoServicoRequest;
import com.gerdau.desafio.dto.response.CodigoServicoResponse;
import com.gerdau.desafio.dto.response.PageResponse;
import com.gerdau.desafio.dto.response.SimilaridadeResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.CodigoServicoMapper;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.Origem;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import com.gerdau.desafio.service.similaridade.SimilaridadeService;
import com.gerdau.desafio.service.similaridade.StatusSimilaridade;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CodigoServicoService {

    private static final Logger log = LoggerFactory.getLogger(CodigoServicoService.class);
    private static final String RECURSO = "Código de Serviço";

    private final CodigoServicoRepository repository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final CodigoServicoMapper mapper;
    private final SimilaridadeService similaridadeService;
    private final GeradorCodigoSequencial gerador;
    private final ConsultaPaginada consulta;

    public CodigoServicoService(CodigoServicoRepository repository, CodigoCompletoRepository codigoCompletoRepository,
                                CodigoServicoMapper mapper, SimilaridadeService similaridadeService,
                                GeradorCodigoSequencial gerador, ConsultaPaginada consulta) {
        this.repository = repository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.mapper = mapper;
        this.similaridadeService = similaridadeService;
        this.gerador = gerador;
        this.consulta = consulta;
    }

    public CodigoServicoResponse criar(CodigoServicoRequest request) {
        return mapper.toResponse(criarEntidade(request.descricao(), request.justificativa()));
    }

    public CodigoServico criarEntidade(String descricao, String justificativa) {
        CodigoServico model = new CodigoServico();
        preencher(model, descricao, justificativa);
        model.setOrigem(Origem.SISTEMA);

        CodigoServico salvo = gerador.inserirComNovoId(model, CodigoServico.COLECAO, CodigoServico.TAMANHO_ID,
                CodigoServico::setId);
        log.info("[CODIGO SERVICO] Criado: id={}, descricao='{}'", salvo.getId(), salvo.getDescricao());
        return salvo;
    }

    public CodigoServicoResponse atualizar(String id, CodigoServicoRequest request) {
        CodigoServico model = buscarEntidade(id);
        preencher(model, request.descricao(), request.justificativa());
        return mapper.toResponse(repository.save(model));
    }

    public void excluir(String id) {
        buscarEntidade(id);
        if (codigoCompletoRepository.existsByCsId(id)) {
            throw new ConflictException("O código de serviço " + id + " está em uso por códigos completos e não pode ser excluído.");
        }
        repository.deleteById(id);
        log.info("[CODIGO SERVICO] Excluído: id={}", id);
    }

    public PageResponse<CodigoServicoResponse> listar(String termo, int page, int size) {
        Query query = new Query();
        if (termo != null && !termo.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    ConsultaPaginada.contem("descricaoNormalizada", TextoUtil.normalizar(termo)),
                    ConsultaPaginada.comecaCom("_id", termo.trim()),
                    Criteria.where("legacyIds").is(termo.trim())));
        }
        return consulta.paginar(query, CodigoServico.class, page, size,
                lista -> lista.stream().map(mapper::toResponse).toList());
    }

    public List<SimilaridadeResponse> buscarSimilares(String descricao, int limite, String ignorarId) {
        return similaridadeService.buscarSimilares(descricao, limite, ignorarId);
    }

    public CodigoServicoResponse buscarPorId(String id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    public CodigoServico buscarEntidade(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
    }

    private void preencher(CodigoServico model, String descricaoBruta, String justificativa) {
        String descricao = TextoUtil.limpar(descricaoBruta).toUpperCase(Locale.ROOT);
        String normalizada = TextoUtil.normalizar(descricao);
        if (normalizada.isEmpty()) {
            throw new BusinessException("A descrição precisa conter letras ou números.");
        }
        if (descricao.length() > TextoUtil.LIMITE_SAP) {
            throw new BusinessException("A descrição não pode exceder 40 caracteres (regra SAP).");
        }

        repository.findByDescricaoNormalizada(normalizada)
                .filter(existente -> !existente.getId().equals(model.getId()))
                .ifPresent(existente -> {
                    throw new ConflictException("Já existe o código de serviço " + existente.getId()
                            + " com a mesma descrição: '" + existente.getDescricao() + "'.");
                });

        boolean semJustificativa = justificativa == null || justificativa.isBlank();
        similaridadeService.maisSimilar(normalizada, model.getId())
                .filter(similar -> similar.status() == StatusSimilaridade.DUPLICIDADE && semJustificativa)
                .ifPresent(similar -> {
                    throw new ConflictException(String.format(Locale.ROOT,
                            "Provável duplicidade com o código %s '%s' (score %.2f). Informe uma justificativa para prosseguir.",
                            similar.id(), similar.descricao(), similar.score()));
                });

        model.setDescricao(descricao);
        model.setDescricaoNormalizada(normalizada);
        model.setQuantidadeCaracteres(descricao.length());
        model.setJustificativa(semJustificativa ? null : justificativa.trim());
    }
}
