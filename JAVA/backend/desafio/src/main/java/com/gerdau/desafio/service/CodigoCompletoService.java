package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CodigoCompletoRequest;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.PageResponse;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.mapper.CodigoCompletoMapper;
import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.Origem;
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import com.gerdau.desafio.repository.CodigoFornecedorRepository;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import com.gerdau.desafio.repository.GrupoServicoRepository;
import com.gerdau.desafio.repository.UnidadeMedidaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CodigoCompletoService {

    private static final Logger log = LoggerFactory.getLogger(CodigoCompletoService.class);
    private static final String RECURSO = "Código Completo";

    private final CodigoCompletoRepository repository;
    private final GrupoServicoRepository grupoRepository;
    private final CodigoFornecedorRepository fornecedorRepository;
    private final CodigoServicoRepository servicoRepository;
    private final UnidadeMedidaRepository unidadeRepository;
    private final GrupoServicoService grupoService;
    private final CodigoFornecedorService fornecedorService;
    private final CodigoServicoService servicoService;
    private final UnidadeMedidaService unidadeService;
    private final CodigoCompletoMapper mapper;
    private final ConsultaPaginada consulta;

    public CodigoCompletoService(CodigoCompletoRepository repository,
                                 GrupoServicoRepository grupoRepository,
                                 CodigoFornecedorRepository fornecedorRepository,
                                 CodigoServicoRepository servicoRepository,
                                 UnidadeMedidaRepository unidadeRepository,
                                 GrupoServicoService grupoService,
                                 CodigoFornecedorService fornecedorService,
                                 CodigoServicoService servicoService,
                                 UnidadeMedidaService unidadeService,
                                 CodigoCompletoMapper mapper,
                                 ConsultaPaginada consulta) {
        this.repository = repository;
        this.grupoRepository = grupoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.servicoRepository = servicoRepository;
        this.unidadeRepository = unidadeRepository;
        this.grupoService = grupoService;
        this.fornecedorService = fornecedorService;
        this.servicoService = servicoService;
        this.unidadeService = unidadeService;
        this.mapper = mapper;
        this.consulta = consulta;
    }

    public CodigoCompletoResponse criar(CodigoCompletoRequest request) {
        GrupoServico grupo = grupoService.buscarEntidade(request.gsId());
        CodigoFornecedor fornecedor = fornecedorService.buscarEntidade(request.cfId());
        UnidadeMedida unidade = unidadeService.buscarEntidade(request.umId());

        if (request.csId() != null && !request.csId().isBlank()) {
            String idCompleto = CodigoCompleto.comporId(grupo.getId(), fornecedor.getId(), request.csId(), unidade.getId());
            garantirInexistente(idCompleto);
        }
        CodigoServico servico = request.csId() != null && !request.csId().isBlank()
                ? servicoService.buscarEntidade(request.csId())
                : servicoService.criarEntidade(request.descricaoServico(), request.justificativa());

        String idCompleto = CodigoCompleto.comporId(grupo.getId(), fornecedor.getId(), servico.getId(), unidade.getId());
        garantirInexistente(idCompleto);

        CodigoCompleto model = new CodigoCompleto();
        model.setId(idCompleto);
        model.setIdCompleto(idCompleto);
        model.setGsId(grupo.getId());
        model.setCfId(fornecedor.getId());
        model.setCsId(servico.getId());
        model.setUmId(unidade.getId());
        model.setDescricaoSnapshot(montarSnapshot(grupo, fornecedor, servico, unidade));
        model.setCreatedAt(Instant.now());
        model.setOrigem(Origem.SISTEMA);

        CodigoCompleto salvo = repository.insert(model);
        log.info("[CODIGO COMPLETO] Criado: id={}", salvo.getId());
        return mapper.toResponse(salvo, grupo, fornecedor, servico, unidade);
    }

    public PageResponse<CodigoCompletoResponse> listar(String gsId, String cfId, String csId, String umId,
                                                       String termo, int page, int size) {
        Query query = new Query();
        adicionarFiltro(query, "gsId", gsId);
        adicionarFiltro(query, "cfId", cfId);
        adicionarFiltro(query, "csId", csId);
        adicionarFiltro(query, "umId", umId);
        if (termo != null && !termo.isBlank()) {
            String t = termo.trim();
            query.addCriteria(new Criteria().orOperator(
                    ConsultaPaginada.contem("descricaoSnapshot", t),
                    ConsultaPaginada.comecaCom("_id", t),
                    Criteria.where("legacyIds").is(t)));
        }
        return consulta.paginar(query, CodigoCompleto.class, page, size, this::enriquecer);
    }

    public CodigoCompletoResponse buscarPorId(String id) {
        CodigoCompleto model = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RECURSO, id));
        return enriquecer(List.of(model)).get(0);
    }

    public List<CodigoCompletoResponse> buscarPorCodigoLegado(String legacyId) {
        List<CodigoCompleto> encontrados = repository.findByLegacyIdsContaining(legacyId.trim());
        if (encontrados.isEmpty()) {
            throw new ResourceNotFoundException("Nenhum código completo associado ao código legado " + legacyId + ".");
        }
        return enriquecer(encontrados);
    }

    public void excluir(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(RECURSO, id);
        }
        repository.deleteById(id);
        log.info("[CODIGO COMPLETO] Excluído: id={}", id);
    }

    static String montarSnapshot(GrupoServico grupo, CodigoFornecedor fornecedor, CodigoServico servico,
                                 UnidadeMedida unidade) {
        return String.format("%s | Fornecedor %d | %s | %s", grupo.getNome(), fornecedor.getCodigoFornecedorSAP(),
                servico.getDescricao(), unidade.getSigla());
    }

    private void garantirInexistente(String idCompleto) {
        if (repository.existsById(idCompleto)) {
            throw new ConflictException("Já existe o código completo " + idCompleto
                    + " para essa combinação de grupo, fornecedor, serviço e unidade.");
        }
    }

    private List<CodigoCompletoResponse> enriquecer(List<CodigoCompleto> codigos) {
        Map<String, GrupoServico> grupos = porId(grupoRepository, codigos, CodigoCompleto::getGsId, GrupoServico::getId);
        Map<String, CodigoFornecedor> fornecedores = porId(fornecedorRepository, codigos, CodigoCompleto::getCfId, CodigoFornecedor::getId);
        Map<String, CodigoServico> servicos = porId(servicoRepository, codigos, CodigoCompleto::getCsId, CodigoServico::getId);
        Map<String, UnidadeMedida> unidades = porId(unidadeRepository, codigos, CodigoCompleto::getUmId, UnidadeMedida::getId);

        return codigos.stream()
                .map(c -> mapper.toResponse(c, grupos.get(c.getGsId()), fornecedores.get(c.getCfId()),
                        servicos.get(c.getCsId()), unidades.get(c.getUmId())))
                .toList();
    }

    private static <T> Map<String, T> porId(CrudRepository<T, String> repo, List<CodigoCompleto> codigos,
                                            Function<CodigoCompleto, String> chave, Function<T, String> id) {
        List<String> ids = codigos.stream().map(chave).filter(Objects::nonNull).distinct().toList();
        return StreamSupport.stream(repo.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(id, Function.identity()));
    }

    private static void adicionarFiltro(Query query, String campo, String valor) {
        if (valor != null && !valor.isBlank()) {
            query.addCriteria(Criteria.where(campo).is(valor.trim()));
        }
    }
}
