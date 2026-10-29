package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.dto.response.AlertaComponenteResponse;
import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.PreviewResponse;
import com.gerdau.desafio.dto.response.SimilaridadeResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.util.ComposicaoUtil;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public CodigoCompletoResponse atualizarPedido(String id, String descricao, String justificativa) {
        CodigoCompleto model = exigirPedidoPendente(id);
        boolean mudou = false;
        if (descricao != null && !descricao.isBlank()) {
            catalogoService.atualizarDescricaoServico(model.getCsId(), descricao);
            model.setDescricaoSnapshot(reconstruirSnapshot(model));
            SimilaridadeResponse sim = similaridadeService.consultar(descricao.trim(), 5, "CS");
            model.setDecisaoSimilaridade(sim.decisao());
            model.setMelhorScore(sim.candidatos().isEmpty() ? 0.0 : sim.candidatos().get(0).score());
            List<RegistroSimilar> matchesComponentes = (model.getSimilaresRegistrados() == null
                    ? List.<RegistroSimilar>of() : model.getSimilaresRegistrados())
                    .stream()
                    .filter(s -> !"CS".equals(s.getTipo()))
                    .toList();
            List<RegistroSimilar> matchesServico = sim.candidatos().stream()
                    .map(CodigoCompletoService::paraRegistro)
                    .toList();
            model.setSimilaresRegistrados(java.util.stream.Stream
                    .concat(matchesServico.stream(), matchesComponentes.stream())
                    .toList());
            mudou = true;
        }
        if (justificativa != null) {
            model.setJustificativa(justificativa.isBlank() ? null : justificativa.trim());
            mudou = true;
        }
        if (!mudou) {
            throw new BusinessException("Informe o que deseja alterar (descrição ou justificativa).");
        }
        CodigoCompleto salvo = codigoCompletoRepository.save(model);
        auditoriaService.registrar(EventoAuditoria.ACAO_ALTERACAO_PEDIDO, "CODIGO_COMPLETO",
                salvo.getIdCompleto(), salvo.getDecisaoSimilaridade(), "Pedido atualizado pelo solicitante.",
                java.util.List.of(), java.util.List.of(), model.getUsuarioCriacaoId());
        log.info("[CODIGO COMPLETO] Pedido atualizado: idCompleto={}", salvo.getIdCompleto());
        return paraResponse(salvo);
    }

    public CodigoCompletoResponse cancelarPedido(String id) {
        String usuario = AuditoriaService.usuarioAtual();
        CodigoCompleto model = exigirPedidoPendente(id);
        model.setStatus(CodigoCompleto.STATUS_CANCELADO);
        model.setAprovadoPor(usuario);
        model.setObservacaoAprovacao("Pedido cancelado pelo solicitante.");
        model.setAprovadoEm(Instant.now());
        model.setIdempotencyKey(null);
        model.setIdempotencyHash(null);
        CodigoCompleto salvo = codigoCompletoRepository.save(model);
        catalogoService.desativarPendentesNaoUsados(model);
        auditoriaService.registrar(EventoAuditoria.ACAO_CANCELAMENTO_PEDIDO, "CODIGO_COMPLETO",
                salvo.getIdCompleto(), salvo.getDecisaoSimilaridade(), "Pedido cancelado pelo solicitante.",
                java.util.List.of(), java.util.List.of(), usuario);
        log.info("[CODIGO COMPLETO] Pedido cancelado: idCompleto={} por {}", salvo.getIdCompleto(), usuario);
        return paraResponse(salvo);
    }

    private CodigoCompleto exigirPedidoPendente(String id) {
        CodigoCompleto model = codigoCompletoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código Completo", id));
        if (!CodigoCompleto.STATUS_PENDENTE.equals(model.getStatus())) {
            throw new BusinessException("Este pedido não está mais pendente (status atual: "
                    + model.getStatus() + ") — não há o que editar ou cancelar.");
        }
        exigirDonoOuAdmin(model);
        return model;
    }

    private void exigirDonoOuAdmin(CodigoCompleto model) {
        if (AuditoriaService.ehAdmin()) {
            return;
        }
        String usuario = AuditoriaService.usuarioAtual();
        String dono = model.getUsuarioCriacaoId() == null ? "" : model.getUsuarioCriacaoId().trim();
        if (usuario.isEmpty() || !usuario.equals(dono)) {
            throw new BusinessException("Somente quem enviou o pedido ("
                    + (dono.isEmpty() ? "sem identificação" : dono)
                    + ") ou um administrador pode editá-lo ou cancelá-lo.");
        }
    }

    private String reconstruirSnapshot(CodigoCompleto model) {
        PreviewService.ComponentesResolvidos r = previewService.resolver(PreviewRequest.deComposicao(
                model.getGsId(), model.getCfId(), model.getCsId(), model.getUmId()), false);
        return previewService.montarSnapshot(r);
    }

    public record ResultadoCriacao(CodigoCompletoResponse resposta, boolean requisicaoRepetida) {
    }

    public ResultadoCriacao criar(CriarCodigoCompletoRequest request, String idempotencyKey) {
        String hashConteudo = hashDoConteudo(request);
        PreviewResponse preview = previewService.montarPreview(paraPreview(request));

        String chave = (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey;
        if (chave != null) {
            CodigoCompleto existente = codigoCompletoRepository.findByIdempotencyKey(chave);
            if (existente != null) {
                boolean mesmaComposicao = preview.idCompleto() != null
                        && preview.idCompleto().equals(existente.getIdCompleto());
                if (!mesmaComposicao) {
                    log.warn("[CODIGO COMPLETO] Idempotency-Key '{}' pertence a outra composição ({}); "
                            + "ignorando a chave neste envio.", chave, existente.getIdCompleto());
                    chave = null;
                } else if (existente.getIdempotencyHash() != null
                        && !existente.getIdempotencyHash().equals(hashConteudo)) {
                    throw new ConflictException("A Idempotency-Key '" + idempotencyKey
                            + "' já foi usada com um conteúdo diferente.");
                } else if (!CodigoCompleto.STATUS_REJEITADO.equals(existente.getStatus())
                        && !CodigoCompleto.STATUS_CANCELADO.equals(existente.getStatus())) {
                    log.info("[CODIGO COMPLETO] Requisição repetida ignorada (key={})", chave);
                    return new ResultadoCriacao(paraResponse(existente), true);
                } else {
                    existente.setIdempotencyKey(null);
                    existente.setIdempotencyHash(null);
                    codigoCompletoRepository.save(existente);
                }
            }
        }

        String acao = PreviewService.ACAO_ENVIAR_VALIDACAO.equalsIgnoreCase(request.acao())
                ? PreviewService.ACAO_ENVIAR_VALIDACAO : PreviewService.ACAO_CRIAR;

        boolean justificado = request.justificativa() != null && !request.justificativa().isBlank();

        String statusComponente = PreviewService.ACAO_ENVIAR_VALIDACAO.equals(acao)
                ? StatusComponente.PENDENTE_VALIDACAO : StatusComponente.ATIVO;
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

        PreviewService.ComponentesResolvidos r = previewService.resolver(paraPreview(request), true);

        if (r.grupoNovo()) {
            catalogoService.exigirGrupoLivre(r.grupoServico().getNome());
        }
        if (r.fornecedorNovo()) {
            catalogoService.exigirFornecedorLivre(r.codigoFornecedor().getId());
        }
        if (r.unidadeNova()) {
            catalogoService.exigirUnidadeLivre(r.unidadeMedida().getSigla());
        }

        String gsId = r.grupoServico().getId();
        String cfId = r.codigoFornecedor().getId();
        String csId = r.codigoServico().getId();
        String umId = r.unidadeMedida().getId();
        if (r.grupoNovo()) {
            gsId = catalogoService.criarGrupo(r.grupoServico().getNome(), statusComponente).id();
        }
        if (r.fornecedorNovo()) {
            cfId = catalogoService.criarFornecedor(r.codigoFornecedor().getNome(),
                    r.codigoFornecedor().getCodigoFornecedorSAP() == null ? null
                            : String.valueOf(r.codigoFornecedor().getCodigoFornecedorSAP()), statusComponente).id();
        }
        if (r.codigoNovo()) {
            csId = catalogoService.criarServico(r.codigoServico().getDescricao(), statusComponente).id();
        }
        if (r.unidadeNova()) {
            umId = catalogoService.criarUnidade(r.unidadeMedida().getSigla(), r.unidadeMedida().getNome(), statusComponente).id();
        }

        if (PreviewService.ACAO_CRIAR.equals(acao) && justificado && !"PERMITIR".equals(preview.decisao())) {
            catalogoService.marcarCasoIsolado(csId, request.justificativa());
        }

        String idCompleto = ComposicaoUtil.montarIdCompleto(gsId, cfId, csId, umId);
        String idRegistro = alocarIdRegistro(idCompleto);
        CodigoCompleto model = new CodigoCompleto();
        model.setId(idRegistro);
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
        model.setSimilaresRegistrados(similaresDoPedido(preview));
        model.setIdempotencyKey(chave);
        model.setIdempotencyHash(chave != null ? hashConteudo : null);

        CodigoCompleto anterior = codigoCompletoRepository.findByIdCompletoOrderByCreatedAtDesc(idCompleto).stream()
                .filter(x -> CodigoCompleto.STATUS_REJEITADO.equals(x.getStatus())
                        || CodigoCompleto.STATUS_CANCELADO.equals(x.getStatus()))
                .findFirst().orElse(null);
        if (anterior != null) {
            String decisaoAnterior = anterior.getStatus() + (anterior.getObservacaoAprovacao() == null
                    || anterior.getObservacaoAprovacao().isBlank() ? "" : " — " + anterior.getObservacaoAprovacao());
            auditoriaService.registrar(EventoAuditoria.ACAO_REENVIO_PEDIDO, "CODIGO_COMPLETO",
                    idCompleto, anterior.getDecisaoSimilaridade(), "Reenvio após decisão anterior: " + decisaoAnterior,
                    java.util.List.of(), java.util.List.of(), request.usuarioCriacaoId());
        }

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
        return new ResultadoCriacao(paraResponse(salvo), false);
    }

    public List<CodigoCompletoResponse> listarTodos(String status) {
        List<CodigoCompleto> itens = (status == null || status.isBlank())
                ? codigoCompletoRepository.findAll()
                : codigoCompletoRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        return itens.stream().map(this::paraResponse).toList();
    }

    public CodigoCompletoResponse buscarPorId(String id) {
        CodigoCompleto model = codigoCompletoRepository.findById(id).orElse(null);
        if (model == null) {
            List<CodigoCompleto> daComposicao = codigoCompletoRepository.findByIdCompletoOrderByCreatedAtDesc(id);
            model = daComposicao.stream()
                    .filter(c -> CodigoCompleto.STATUS_PENDENTE.equals(c.getStatus()))
                    .findFirst()
                    .orElseGet(() -> daComposicao.stream().findFirst().orElse(null));
        }
        if (model == null) {
            throw new ResourceNotFoundException("Código Completo", id);
        }
        return paraResponse(model);
    }

    private String alocarIdRegistro(String idCompleto) {
        if (codigoCompletoRepository.findById(idCompleto).isEmpty()) {
            return idCompleto;
        }
        for (int n = 2; ; n++) {
            String candidato = idCompleto + "#" + n;
            if (codigoCompletoRepository.findById(candidato).isEmpty()) {
                return candidato;
            }
        }
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

    private static List<RegistroSimilar> similaresDoPedido(PreviewResponse preview) {
        List<RegistroSimilar> similares = new java.util.ArrayList<>(preview.similares().stream()
                .map(CodigoCompletoService::paraRegistro)
                .toList());
        for (AlertaComponenteResponse a : preview.alertasComponentes()) {
            similares.add(new RegistroSimilar(similares.size() + 1, a.itemSimilarId(), null, a.itemSimilarNome(),
                    a.score(), PreviewService.nomeComponente(a.tipo()) + ": “" + a.descricaoInformada()
                    + "” é semelhante a este item.", a.casoIsolado(), a.motivoCasoIsolado(), a.tipo()));
        }
        return similares;
    }

    private static RegistroSimilar paraRegistro(CandidatoSimilarResponse c) {
        return new RegistroSimilar(c.posicao(), c.id(), c.codigoOriginal(), c.descricao(), c.score(), c.motivo(),
                c.casoIsolado(), c.motivoCasoIsolado(), c.tipo());
    }

    private static PreviewRequest paraPreview(CriarCodigoCompletoRequest r) {
        return new PreviewRequest(r.gsId(), r.gsNovoNome(), r.cfId(), r.cfNovoNome(), r.cfNovoCodigoSap(),
                r.csId(), r.csNovaDescricao(), r.umId(), r.umNovaSigla(), r.umNovoNome());
    }

    public static String hashDoConteudo(CriarCodigoCompletoRequest r) {
        String canonico = String.join("|",
                TextoUtil.nvl(r.gsId()), TextoUtil.nvl(r.gsNovoNome()), TextoUtil.nvl(r.cfId()), TextoUtil.nvl(r.cfNovoNome()), TextoUtil.nvl(r.cfNovoCodigoSap()),
                TextoUtil.nvl(r.csId()), TextoUtil.nvl(r.csNovaDescricao()), TextoUtil.nvl(r.umId()), TextoUtil.nvl(r.umNovaSigla()), TextoUtil.nvl(r.umNovoNome()),
                TextoUtil.nvl(r.acao()), TextoUtil.nvl(r.justificativa()));
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
