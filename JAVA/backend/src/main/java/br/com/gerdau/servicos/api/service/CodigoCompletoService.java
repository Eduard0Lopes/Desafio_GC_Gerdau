package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.config.AppProperties;
import br.com.gerdau.servicos.api.domain.DecisaoSimilaridade;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.CodigoCompletoDtos.CodigoCompletoResponse;
import br.com.gerdau.servicos.api.dto.CodigoCompletoDtos.ComposicaoRequest;
import br.com.gerdau.servicos.api.dto.CodigoCompletoDtos.PreviewResponse;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilaridadeResponse;
import br.com.gerdau.servicos.api.persistence.CodigoCompletoRepository;
import br.com.gerdau.servicos.api.persistence.ComponenteRepository;
import br.com.gerdau.servicos.api.text.IdFormatter;
import br.com.gerdau.servicos.api.web.ApiException;
import br.com.gerdau.servicos.api.web.AuditInfo;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static br.com.gerdau.servicos.api.persistence.DocumentSupport.id;
import static br.com.gerdau.servicos.api.persistence.DocumentSupport.str;

/** Composição do código completo G.S. + C.F. + C.S. + U.M. (doc. seções 7.5, 8.3, 8.4 e 10). */
@Service
public class CodigoCompletoService {

    private record Composicao(String gsId, String cfId, String csId, String umId, String idCompleto,
                              String snapshot, String csDescricao) {}

    private final ComponenteRepository componentes;
    private final CodigoCompletoRepository codigos;
    private final SimilarityService similarity;
    private final IdempotencyService idempotency;
    private final IdFormatter ids;
    private final AppProperties props;
    private final Clock clock;

    public CodigoCompletoService(ComponenteRepository componentes, CodigoCompletoRepository codigos,
                                 SimilarityService similarity, IdempotencyService idempotency,
                                 IdFormatter ids, AppProperties props, Clock clock) {
        this.componentes = componentes;
        this.codigos = codigos;
        this.similarity = similarity;
        this.idempotency = idempotency;
        this.ids = ids;
        this.props = props;
        this.clock = clock;
    }

    public PreviewResponse preview(ComposicaoRequest req) {
        Composicao c = carregar(req);
        int tamanho = c.csDescricao().codePointCount(0, c.csDescricao().length());
        int limite = props.limits().descricaoMax();
        boolean existe = existe(c);
        SimilaridadeResponse sim = similarity.avaliar(TipoComponente.CS, c.csDescricao(), c.csId());
        DecisaoSimilaridade recomendada = existe || tamanho > limite ? DecisaoSimilaridade.REVISAR : sim.decisao();
        return new PreviewResponse(c.idCompleto(), c.gsId(), c.cfId(), c.csId(), c.umId(), c.snapshot(),
                tamanho, limite, tamanho <= limite, existe, recomendada, sim);
    }

    public CodigoCompletoResponse criar(ComposicaoRequest req, AuditInfo audit, String idempotencyKey) {
        return idempotency.execute(idempotencyKey, audit.usuario(), "codigos-completos", req,
                CodigoCompletoResponse.class, () -> criarInterno(req, audit));
    }

    public CodigoCompletoResponse buscarPorId(String idCompleto) {
        Document d = codigos.findById(idCompleto).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "CODIGO_NAO_ENCONTRADO", "Código completo '" + idCompleto + "' não encontrado."));
        return toResponse(d);
    }

    private CodigoCompletoResponse criarInterno(ComposicaoRequest req, AuditInfo audit) {
        Composicao c = carregar(req);
        int tamanho = c.csDescricao().codePointCount(0, c.csDescricao().length());
        int limite = props.limits().descricaoMax();
        if (tamanho > limite) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DESCRICAO_ACIMA_DO_LIMITE",
                    "A descrição do Código de Serviço tem " + tamanho + " caracteres; o limite do SAP é " + limite + ".",
                    Map.of("limite", limite, "tamanho", tamanho));
        }
        if (existe(c)) {
            throw jaExiste(c);
        }
        Document doc = new Document("_id", c.idCompleto())
                .append("idCompleto", c.idCompleto())
                .append("gsId", c.gsId()).append("cfId", c.cfId())
                .append("csId", c.csId()).append("umId", c.umId())
                .append("descricaoSnapshot", c.snapshot())
                .append("usuarioCriacaoId", audit.usuario())
                .append("createdAt", Date.from(clock.instant()))
                .append("origem", "APLICACAO")
                .append("legacyIds", List.of());
        if (audit.ip() != null) {
            doc.append("usuarioCriacaoIp", audit.ip());
        }
        try {
            codigos.insert(doc);
        } catch (DuplicateKeyException e) {
            // Corrida entre duas requisições: o índice único do banco é a palavra final (doc. 8.4).
            throw jaExiste(c);
        }
        return toResponse(doc);
    }

    private boolean existe(Composicao c) {
        return codigos.existsById(c.idCompleto())
                || codigos.existsByComposicao(c.gsId(), c.cfId(), c.csId(), c.umId());
    }

    private ApiException jaExiste(Composicao c) {
        return new ApiException(HttpStatus.CONFLICT, "CODIGO_COMPLETO_JA_EXISTE",
                "O código completo " + c.idCompleto() + " já está cadastrado.", Map.of("idCompleto", c.idCompleto()));
    }

    private Composicao carregar(ComposicaoRequest r) {
        Document gs = obter(TipoComponente.GS, "gsId", r.gsId());
        Document cf = obter(TipoComponente.CF, "cfId", r.cfId());
        Document cs = obter(TipoComponente.CS, "csId", r.csId());
        Document um = obter(TipoComponente.UM, "umId", r.umId());
        String gsId;
        String cfId;
        String csId;
        String umId;
        try {
            gsId = ids.canonical(TipoComponente.GS, id(gs));
            cfId = ids.canonical(TipoComponente.CF, id(cf));
            csId = ids.canonical(TipoComponente.CS, id(cs));
            umId = ids.canonical(TipoComponente.UM, id(um));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ID_FORA_DO_PADRAO", e.getMessage());
        }
        String umRotulo = str(um, "sigla").isBlank() ? str(um, "nome") : str(um, "sigla");
        String csDescricao = str(cs, TipoComponente.CS.textField());
        String snapshot = str(gs, "nome") + " | " + str(cf, "nome") + " | " + csDescricao + " | " + umRotulo;
        return new Composicao(gsId, cfId, csId, umId, gsId + cfId + csId + umId, snapshot, csDescricao);
    }

    private Document obter(TipoComponente tipo, String campo, String id) {
        return componentes.findById(tipo, id.trim()).orElseThrow(() -> new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY, "COMPONENTE_INEXISTENTE",
                tipo.label() + " '" + id + "' não encontrado.", Map.of("campo", campo)));
    }

    private CodigoCompletoResponse toResponse(Document d) {
        Object created = d.get("createdAt");
        Instant createdAt = created instanceof Date date ? date.toInstant() : null;
        return new CodigoCompletoResponse(str(d, "idCompleto").isBlank() ? id(d) : str(d, "idCompleto"),
                str(d, "gsId"), str(d, "cfId"), str(d, "csId"), str(d, "umId"),
                str(d, "descricaoSnapshot"), d.get("usuarioCriacaoId") == null ? null : str(d, "usuarioCriacaoId"),
                createdAt);
    }
}
