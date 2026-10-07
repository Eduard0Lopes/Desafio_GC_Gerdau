package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.domain.SolicitacaoGovernanca;
import br.com.gerdau.servicos.api.domain.SolicitacaoGovernanca.Status;
import br.com.gerdau.servicos.api.dto.GovernancaDtos.Decisao;
import br.com.gerdau.servicos.api.dto.GovernancaDtos.DecisaoRequest;
import br.com.gerdau.servicos.api.dto.GovernancaDtos.SolicitacaoResponse;
import br.com.gerdau.servicos.api.dto.GovernancaDtos.SolicitarGovernancaRequest;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilarItem;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilaridadeResponse;
import br.com.gerdau.servicos.api.persistence.SolicitacaoGovernancaRepository;
import br.com.gerdau.servicos.api.service.ItemValidator.ItemValido;
import br.com.gerdau.servicos.api.web.ApiException;
import br.com.gerdau.servicos.api.web.AuditInfo;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluxo de Governança (doc. 11.8): quando o item é muito parecido com outro, o solicitante justifica e uma
 * pessoa da Equipe de Governança de Dados aprova ou rejeita. Quem solicitou não pode aprovar o próprio pedido.
 */
@Service
public class GovernancaService {

    private final SolicitacaoGovernancaRepository repository;
    private final ItemValidator validator;
    private final SimilarityService similarity;
    private final ItemService itemService;
    private final IdempotencyService idempotency;
    private final Clock clock;

    public GovernancaService(SolicitacaoGovernancaRepository repository, ItemValidator validator,
                             SimilarityService similarity, ItemService itemService,
                             IdempotencyService idempotency, Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.similarity = similarity;
        this.itemService = itemService;
        this.idempotency = idempotency;
        this.clock = clock;
    }

    public SolicitacaoResponse solicitar(SolicitarGovernancaRequest req, AuditInfo audit, String idempotencyKey) {
        return idempotency.execute(idempotencyKey, audit.usuario(), "governanca", req, SolicitacaoResponse.class,
                () -> solicitarInterno(req, audit));
    }

    private SolicitacaoResponse solicitarInterno(SolicitarGovernancaRequest req, AuditInfo audit) {
        ItemValido item = validator.validar(req.tipo(), req.texto(), req.sigla());
        SimilaridadeResponse sim = similarity.avaliar(item.tipo(), item.texto(), null);

        List<Map<String, Object>> similares = new ArrayList<>();
        for (SimilarItem s : sim.similares()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.id());
            m.put("texto", s.texto());
            m.put("score", s.score());
            m.put("motivo", s.motivo());
            similares.add(m);
        }
        SolicitacaoGovernanca s = new SolicitacaoGovernanca();
        s.setTipo(item.tipo());
        s.setTexto(item.texto());
        s.setSigla(item.sigla());
        s.setJustificativa(req.justificativa().strip());
        s.setStatus(Status.PENDENTE);
        s.setSolicitanteId(audit.usuario());
        s.setCreatedAt(clock.instant());
        s.setScoreMaximo(sim.scoreMaximo());
        s.setSimilares(similares);
        return SolicitacaoResponse.from(repository.save(s));
    }

    public List<SolicitacaoResponse> listar(Status status, int limit) {
        return repository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(0, limit)).stream()
                .map(SolicitacaoResponse::from).toList();
    }

    public SolicitacaoResponse decidir(String id, DecisaoRequest req, AuditInfo audit) {
        SolicitacaoGovernanca s = repository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "SOLICITACAO_NAO_ENCONTRADA", "Solicitação '" + id + "' não encontrada."));
        if (s.getStatus() != Status.PENDENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "SOLICITACAO_JA_DECIDIDA",
                    "Esta solicitação já foi " + s.getStatus().name().toLowerCase() + ".");
        }
        if (audit.usuario().equals(s.getSolicitanteId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTOAPROVACAO_NAO_PERMITIDA",
                    "Quem solicitou não pode decidir o próprio pedido.");
        }
        String observacao = req.observacao() == null ? "" : req.observacao().strip();
        if (req.decisao() == Decisao.REJEITAR) {
            if (observacao.length() < 5) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "OBSERVACAO_OBRIGATORIA",
                        "Informe o motivo da rejeição (mínimo 5 caracteres).");
            }
            s.setStatus(Status.REJEITADA);
            s.setItemSugeridoId(req.itemSugeridoId());
        } else {
            ItemValido item = validator.validar(s.getTipo(), s.getTexto(), s.getSigla());
            String criadoId = itemService.criarPorGovernanca(item, s.getSolicitanteId(), audit.usuario(),
                    s.getJustificativa(), observacao.isEmpty() ? null : observacao);
            s.setStatus(Status.APROVADA);
            s.setItemCriadoId(criadoId);
        }
        s.setDecisorId(audit.usuario());
        s.setDecididoEm(clock.instant());
        s.setObservacaoDecisao(observacao.isEmpty() ? null : observacao);
        // @Version: duas decisões simultâneas -> a segunda falha com OptimisticLockingFailureException (409).
        return SolicitacaoResponse.from(repository.save(s));
    }
}
