package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.PreviewResponse;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Criação de códigos completos com governança de entrada (seções 10.2, 10.3 e 12):
 * prévia → decisão → bloqueio/justificativa auditada/envio para validação + idempotência.
 */
@Service
public class CodigoCompletoService {

    private static final Logger log = LoggerFactory.getLogger(CodigoCompletoService.class);

    private final CodigoCompletoRepository codigoCompletoRepository;
    private final ExcecaoRepository excecaoRepository;
    private final PreviewService previewService;
    private final CatalogoService catalogoService;
    private final SimilaridadeService similaridadeService;
    private final AuditoriaService auditoriaService;
    private final boolean modoEnforce;

    public CodigoCompletoService(CodigoCompletoRepository codigoCompletoRepository,
                                 ExcecaoRepository excecaoRepository,
                                 PreviewService previewService,
                                 CatalogoService catalogoService,
                                 SimilaridadeService similaridadeService,
                                 AuditoriaService auditoriaService,
                                 @Value("${similaridade.modo:enforce}") String modo) {
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.excecaoRepository = excecaoRepository;
        this.previewService = previewService;
        this.catalogoService = catalogoService;
        this.similaridadeService = similaridadeService;
        this.auditoriaService = auditoriaService;
        this.modoEnforce = !"observe".equalsIgnoreCase(modo);
    }

    public PreviewResponse preview(PreviewRequest request) {
        return previewService.montarPreview(request);
    }

    public CodigoCompletoResponse criar(CriarCodigoCompletoRequest request, String idempotencyKey) {
        String hashConteudo = hashDoConteudo(request);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            CodigoCompleto existente = codigoCompletoRepository.findByIdempotencyKey(idempotencyKey);
            if (existente != null) {
                // mesma chave + conteúdo diferente = conflito (especificação, seção 7)
                if (existente.getIdempotencyHash() != null
                        && !existente.getIdempotencyHash().equals(hashConteudo)) {
                    throw new ConflictException("A Idempotency-Key '" + idempotencyKey
                            + "' já foi usada com um conteúdo diferente.");
                }
                log.info("[CODIGO COMPLETO] Requisição repetida ignorada (key={})", idempotencyKey);
                return paraResponse(existente);
            }
        }

        PreviewResponse preview = previewService.montarPreview(paraPreview(request));

        if (preview.idCompletoJaExiste()) {
            throw new ConflictException("Já existe um código completo com a composição "
                    + preview.idCompleto() + ". Reutilize o registro existente.");
        }

        String acao = PreviewService.ACAO_ENVIAR_VALIDACAO.equalsIgnoreCase(request.acao())
                ? PreviewService.ACAO_ENVIAR_VALIDACAO : PreviewService.ACAO_CRIAR;

        boolean justificado = request.justificativa() != null && !request.justificativa().isBlank();
        boolean bloqueado = modoEnforce
                && PreviewService.ACAO_CRIAR.equals(acao)
                && !"PERMITIR".equals(preview.decisao())
                && !justificado;

        if (bloqueado) {
            String motivo = "Criação bloqueada pela política de similaridade ("
                    + preview.decisao() + ", score " + preview.melhorScore() + ").";
            auditoriaService.registrar(EventoAuditoria.ACAO_BLOQUEIO, "CODIGO_COMPLETO",
                    preview.idCompleto(), preview.decisao(), motivo,
                    idsDosSimilares(preview), scoresDosSimilares(preview), request.usuarioCriacaoId());
            throw new ConflictException(motivo
                    + " Reutilize um dos itens sugeridos, envie para validação ou informe uma justificativa.");
        }

        // Persiste componentes novos e materializa os IDs oficiais
        PreviewService.ComponentesResolvidos r = previewService.resolver(paraPreview(request), true);
        String gsId = r.grupoServico().getId();
        String cfId = r.codigoFornecedor().getId();
        String csId = r.codigoServico().getId();
        String umId = r.unidadeMedida().getId();
        if (r.grupoNovo()) {
            gsId = catalogoService.criarGrupo(r.grupoServico().getNome()).id();
        }
        if (r.fornecedorNovo()) {
            cfId = catalogoService.criarFornecedor(r.codigoFornecedor().getNome(),
                    r.codigoFornecedor().getCodigoFornecedorSAP() == null ? null
                            : String.valueOf(r.codigoFornecedor().getCodigoFornecedorSAP())).id();
        }
        if (r.codigoNovo()) {
            csId = catalogoService.criarServico(r.codigoServico().getDescricao()).id();
        }
        if (r.unidadeNova()) {
            umId = catalogoService.criarUnidade(r.unidadeMedida().getSigla(), r.unidadeMedida().getNome()).id();
        }

        String idCompleto = gsId + "_" + cfId + "_" + csId + "_" + umId;
        CodigoCompleto model = new CodigoCompleto();
        model.setId(idCompleto);
        model.setIdCompleto(idCompleto);
        model.setGsId(gsId);
        model.setCfId(cfId);
        model.setCsId(csId);
        model.setUmId(umId);
        model.setDescricaoSnapshot(previewService.montarSnapshot(r));
        model.setUsuarioCriacaoId(request.usuarioCriacaoId());
        model.setCreatedAt(Instant.now().toString());
        model.setOrigem("APLICACAO");
        model.setLegacyIds(List.of());
        model.setStatus(PreviewService.ACAO_ENVIAR_VALIDACAO.equals(acao)
                ? CodigoCompleto.STATUS_PENDENTE : CodigoCompleto.STATUS_ATIVO);
        model.setDecisaoSimilaridade(preview.decisao());
        model.setMelhorScore(preview.melhorScore());
        model.setJustificativa(request.justificativa());
        model.setSimilaresRegistrados(preview.similares().stream()
                .map(c -> new RegistroSimilar(c.posicao(), c.id(), c.codigoOriginal(), c.descricao(), c.score(), c.motivo()))
                .toList());
        model.setIdempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null);
        model.setIdempotencyHash(idempotencyKey != null && !idempotencyKey.isBlank() ? hashConteudo : null);

        CodigoCompleto salvo = codigoCompletoRepository.save(model);

        auditoriaService.registrar(EventoAuditoria.ACAO_CRIACAO_CODIGO_COMPLETO, "CODIGO_COMPLETO",
                salvo.getIdCompleto(), preview.decisao(),
                "Criação registrada (" + salvo.getStatus() + ")",
                idsDosSimilares(preview), scoresDosSimilares(preview), request.usuarioCriacaoId());

        if (justificado && !"PERMITIR".equals(preview.decisao())) {
            registrarExcecao(request, preview, salvo);
            auditoriaService.registrar(EventoAuditoria.ACAO_EXCECAO, "CODIGO_COMPLETO",
                    salvo.getIdCompleto(), preview.decisao(),
                    request.justificativa(), idsDosSimilares(preview), scoresDosSimilares(preview),
                    request.usuarioCriacaoId());
        }

        log.info("[CODIGO COMPLETO] Criado: idCompleto={}, status={}, decisao={}, score={}",
                salvo.getIdCompleto(), salvo.getStatus(), salvo.getDecisaoSimilaridade(), salvo.getMelhorScore());
        return paraResponse(salvo);
    }

    public List<CodigoCompletoResponse> listarTodos(String status) {
        List<CodigoCompleto> itens = (status == null || status.isBlank())
                ? codigoCompletoRepository.findAll()
                : codigoCompletoRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        return itens.stream().map(this::paraResponse).toList();
    }

    public CodigoCompletoResponse buscarPorId(String id) {
        CodigoCompleto model = codigoCompletoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código Completo", id));
        return paraResponse(model);
    }

    private void registrarExcecao(CriarCodigoCompletoRequest request, PreviewResponse preview, CodigoCompleto salvo) {
        Excecao ex = new Excecao();
        ex.setSolicitante(request.usuarioCriacaoId());
        ex.setDataHora(Instant.now());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idCompleto", salvo.getIdCompleto());
        payload.put("descricaoServico", salvo.getCsId());
        payload.put("composicao", Map.of("gsId", salvo.getGsId(), "cfId", salvo.getCfId(),
                "csId", salvo.getCsId(), "umId", salvo.getUmId()));
        ex.setPayloadOriginal(payload);
        ex.setSimilaresEncontrados(salvo.getSimilaresRegistrados());
        ex.setJustificativa(request.justificativa());
        ex.setDecisaoTomada("CRIADO_COM_JUSTIFICATIVA (" + preview.decisao() + ")");
        ex.setObservacao(preview.acaoRecomendada());
        ex.setCodigoCompletoId(salvo.getIdCompleto());
        excecaoRepository.save(ex);
    }

    private static PreviewRequest paraPreview(CriarCodigoCompletoRequest r) {
        return new PreviewRequest(r.gsId(), r.gsNovoNome(), r.cfId(), r.cfNovoNome(), r.cfNovoCodigoSap(),
                r.csId(), r.csNovaDescricao(), r.umId(), r.umNovaSigla(), r.umNovoNome());
    }

    /** Hash SHA-256 do conteúdo da requisição (idempotência — seção 7). */
    public static String hashDoConteudo(CriarCodigoCompletoRequest r) {
        String canonico = String.join("|",
                nvl(r.gsId()), nvl(r.gsNovoNome()), nvl(r.cfId()), nvl(r.cfNovoNome()), nvl(r.cfNovoCodigoSap()),
                nvl(r.csId()), nvl(r.csNovaDescricao()), nvl(r.umId()), nvl(r.umNovaSigla()), nvl(r.umNovoNome()),
                nvl(r.acao()), nvl(r.justificativa()));
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonico.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(canonico.hashCode());
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    private static java.util.List<String> idsDosSimilares(PreviewResponse p) {
        return p.similares().stream().map(c -> c.id()).toList();
    }

    private static java.util.List<Double> scoresDosSimilares(PreviewResponse p) {
        return p.similares().stream().map(c -> c.score()).toList();
    }

    private CodigoCompletoResponse paraResponse(CodigoCompleto m) {
        return new CodigoCompletoResponse(m.getId(), m.getIdCompleto(), m.getGsId(), m.getCfId(), m.getCsId(),
                m.getUmId(), m.getDescricaoSnapshot(), m.getOrigem(),
                m.getLegacyIds() == null ? List.of() : m.getLegacyIds(),
                m.getStatus(), m.getDecisaoSimilaridade(), m.getMelhorScore(), m.getJustificativa(),
                m.getUsuarioCriacaoId(), m.getCreatedAt(), m.getAprovadoPor(), m.getObservacaoAprovacao());
    }
}
