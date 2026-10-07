package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.config.AppProperties;
import br.com.gerdau.servicos.api.config.AppProperties.Enforcement;
import br.com.gerdau.servicos.api.domain.DecisaoSimilaridade;
import br.com.gerdau.servicos.api.domain.ExcecaoSimilaridade;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.ItemDtos.CriarItemRequest;
import br.com.gerdau.servicos.api.dto.ItemDtos.ItemCriadoResponse;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilarItem;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilaridadeResponse;
import br.com.gerdau.servicos.api.persistence.ComponenteRepository;
import br.com.gerdau.servicos.api.persistence.ExcecaoSimilaridadeRepository;
import br.com.gerdau.servicos.api.service.ItemValidator.ItemValido;
import br.com.gerdau.servicos.api.text.TextNormalizer;
import br.com.gerdau.servicos.api.web.ApiException;
import br.com.gerdau.servicos.api.web.AuditInfo;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cadastro de novo item (GS, CF, CS ou UM) com a política de reutilização antes de criação:
 * <pre>
 *  PERMITIR -> cria
 *  ALERTAR  -> exige justificativa (registra exceção) ou o usuário reutiliza
 *  REVISAR  -> bloqueia: reutilizar, corrigir ou enviar para a Governança
 * </pre>
 * Com SIMILARITY_ENFORCEMENT=observe nada bloqueia: os candidatos só são registrados (doc. 12.4).
 */
@Service
public class ItemService {

    static final int MIN_JUSTIFICATIVA = 10;
    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final SimilarityService similarity;
    private final ItemValidator validator;
    private final ComponenteRepository repo;
    private final ComponenteMapper mapper;
    private final SequenceService sequences;
    private final TextNormalizer normalizer;
    private final ExcecaoSimilaridadeRepository excecoes;
    private final IdempotencyService idempotency;
    private final AppProperties props;
    private final Clock clock;

    public ItemService(SimilarityService similarity, ItemValidator validator, ComponenteRepository repo,
                       ComponenteMapper mapper, SequenceService sequences, TextNormalizer normalizer,
                       ExcecaoSimilaridadeRepository excecoes, IdempotencyService idempotency,
                       AppProperties props, Clock clock) {
        this.similarity = similarity;
        this.validator = validator;
        this.repo = repo;
        this.mapper = mapper;
        this.sequences = sequences;
        this.normalizer = normalizer;
        this.excecoes = excecoes;
        this.idempotency = idempotency;
        this.props = props;
        this.clock = clock;
    }

    public ItemCriadoResponse criar(CriarItemRequest req, AuditInfo audit, String idempotencyKey) {
        return idempotency.execute(idempotencyKey, audit.usuario(), "itens", req, ItemCriadoResponse.class,
                () -> criarInterno(req, audit));
    }

    private ItemCriadoResponse criarInterno(CriarItemRequest req, AuditInfo audit) {
        ItemValido item = validator.validar(req.tipo(), req.texto(), req.sigla());
        SimilaridadeResponse sim = similarity.avaliar(item.tipo(), item.texto(), null);
        boolean enforce = props.similarity().enforcement() == Enforcement.ENFORCE;
        String justificativa = req.justificativa() == null ? "" : req.justificativa().strip();

        if (enforce && sim.decisao() == DecisaoSimilaridade.REVISAR) {
            throw new ApiException(HttpStatus.CONFLICT, "SIMILARIDADE_ALTA",
                    "Já existe item muito semelhante. Reutilize o existente, corrija o texto ou envie para a Governança.",
                    Map.of("similaridade", sim));
        }
        if (enforce && sim.decisao() == DecisaoSimilaridade.ALERTAR && justificativa.length() < MIN_JUSTIFICATIVA) {
            throw new ApiException(HttpStatus.CONFLICT, "SIMILARIDADE_INTERMEDIARIA",
                    "Existem itens semelhantes. Reutilize um deles ou informe uma justificativa (mínimo "
                            + MIN_JUSTIFICATIVA + " caracteres) para criar mesmo assim.",
                    Map.of("similaridade", sim));
        }

        Document doc = persistir(item, audit.usuario());
        boolean registrouExcecao = false;
        if (sim.decisao() != DecisaoSimilaridade.PERMITIR) {
            registrarExcecao(item, sim, justificativa, enforce ? "PROSSEGUIU_COM_JUSTIFICATIVA" : "OBSERVACAO",
                    audit.usuario(), null, String.valueOf(doc.get("_id")));
            registrouExcecao = true;
        }
        return new ItemCriadoResponse(mapper.toDto(item.tipo(), doc, null, false), sim.decisao(), registrouExcecao);
    }

    /** Criação após aprovação humana da Governança: ignora a trava de similaridade, mas deixa rastro. */
    public String criarPorGovernanca(ItemValido item, String solicitanteId, String decisorId,
                                     String justificativaSolicitacao, String observacaoDecisao) {
        SimilaridadeResponse sim = similarity.avaliar(item.tipo(), item.texto(), null);
        Document doc = persistir(item, solicitanteId);
        String id = String.valueOf(doc.get("_id"));
        registrarExcecao(item, sim, justificativaSolicitacao, "APROVADO_GOVERNANCA", solicitanteId,
                "Aprovado por " + decisorId + (observacaoDecisao == null ? "" : ": " + observacaoDecisao), id);
        return id;
    }

    private Document persistir(ItemValido item, String usuario) {
        TipoComponente tipo = item.tipo();
        String normalizado = normalizer.normalize(item.texto());
        Document doc = new Document();
        doc.append(tipo.textField(), item.texto());
        doc.append(tipo.normalizedField(), normalizado);
        if (tipo == TipoComponente.CS) {
            doc.append("quantidadeCaracteres", item.texto().codePointCount(0, item.texto().length()));
            doc.append("legacyIds", List.of());
        }
        if (tipo == TipoComponente.UM) {
            doc.append("sigla", item.sigla());
        }
        doc.append("origem", "APLICACAO");
        doc.append("usuarioCriacaoId", usuario);
        doc.append("createdAt", Date.from(clock.instant()));

        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            doc.put("_id", sequences.proximoId(tipo));
            try {
                repo.insert(tipo, doc);
                return doc;
            } catch (DuplicateKeyException e) {
                log.warn("ID {} já existia em {} (tentativa {}); pegando o próximo.", doc.get("_id"), tipo.collection(), tentativa);
            }
        }
        throw new IllegalStateException("Não foi possível gerar um ID livre para " + tipo);
    }

    private void registrarExcecao(ItemValido item, SimilaridadeResponse sim, String justificativa, String decisaoTomada,
                                  String solicitante, String observacao, String itemCriadoId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipo", item.tipo().name());
        payload.put("texto", item.texto());
        if (item.sigla() != null) payload.put("sigla", item.sigla());

        List<Map<String, Object>> similares = new ArrayList<>();
        for (SimilarItem s : sim.similares()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.id());
            m.put("texto", s.texto());
            m.put("score", s.score());
            m.put("motivo", s.motivo());
            similares.add(m);
        }
        ExcecaoSimilaridade e = new ExcecaoSimilaridade();
        e.setTipo(item.tipo());
        e.setPayload(payload);
        e.setSimilares(similares);
        e.setScoreMaximo(sim.scoreMaximo());
        e.setDecisaoCalculada(sim.decisao());
        e.setDecisaoTomada(decisaoTomada);
        e.setJustificativa(justificativa == null || justificativa.isBlank() ? null : justificativa);
        e.setObservacao(observacao);
        e.setSolicitanteId(solicitante);
        e.setItemCriadoId(itemCriadoId);
        e.setCreatedAt(clock.instant());
        excecoes.save(e);
    }
}
