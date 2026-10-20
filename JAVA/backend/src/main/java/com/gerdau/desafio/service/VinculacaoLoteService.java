package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.request.VinculacaoLoteRequest;
import com.gerdau.desafio.dto.request.VinculacaoLoteRequest.ItemVinculacaoLote;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.ItemLoteResponse;
import com.gerdau.desafio.dto.response.ItemVinculacaoResponse;
import com.gerdau.desafio.dto.response.LoteSimilaridadeResponse;
import com.gerdau.desafio.dto.response.LoteVinculacaoResponse;
import com.gerdau.desafio.model.CodigoCompleto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vinculação em massa (feedback Gerdau): o usuário escolhe Grupo de Serviço,
 * Código de Fornecedor e Unidade de Medida uma vez e marca vários Códigos de
 * Serviço com checkbox — o sistema vincula todos de uma vez.
 *
 * As regras de negócio valem para a lista inteira, linha a linha:
 *   - código existente (ou duplicidade provável) → REUTILIZA o existente;
 *   - código novo sem parecidos            → CRIA e vincula ativo;
 *   - código novo com similaridade alertada → cria o pedido e envia para a
 *     GOVERNANÇA (que vê os matches que o usuário não escolheu e decide);
 *   - linha vazia/repetida/fora do limite SAP → não entra, com o motivo.
 * Uma linha que falha não derruba as demais.
 */
@Service
public class VinculacaoLoteService {

    private static final Logger log = LoggerFactory.getLogger(VinculacaoLoteService.class);

    static final String JUSTIFICATIVA_LOTE =
            "Vinculação em massa — item com similaridade alertada; encaminhado para validação da Governança.";

    private final CodigoCompletoService codigoCompletoService;
    private final SimilaridadeService similaridadeService;

    public VinculacaoLoteService(CodigoCompletoService codigoCompletoService,
                                 SimilaridadeService similaridadeService) {
        this.codigoCompletoService = codigoCompletoService;
        this.similaridadeService = similaridadeService;
    }

    public LoteVinculacaoResponse vincularEmLote(VinculacaoLoteRequest request, String idempotencyKeyBase) {
        List<ItemVinculacaoLote> itens = request.itens() == null ? List.of() : request.itens();
        Map<Integer, ItemLoteResponse> analises = avaliarNovos(itens);

        List<ItemVinculacaoResponse> resultados = new ArrayList<>();
        for (int i = 0; i < itens.size(); i++) {
            resultados.add(processarLinha(request, i + 1, itens.get(i), analises.get(i), idempotencyKeyBase));
        }

        LoteVinculacaoResponse resposta = consolidar(resultados);
        log.info("[VINCULACAO/Lote] total={} disponiveis={} criados={} vinculados={} "
                        + "aguardandoValidacao={} bloqueadas={} falhas={}",
                resposta.total(), resposta.disponiveis(), resposta.criados(), resposta.vinculados(),
                resposta.aguardandoValidacao(), resposta.bloqueadas(), resposta.falhas());
        return resposta;
    }

    /** Os códigos novos passam pelas MESMAS regras da tela de informação (similaridade/lote). */
    private Map<Integer, ItemLoteResponse> avaliarNovos(List<ItemVinculacaoLote> itens) {
        List<Integer> posicoes = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        for (int i = 0; i < itens.size(); i++) {
            ItemVinculacaoLote item = itens.get(i);
            if (vazio(item.csId())) {
                posicoes.add(i);
                textos.add(item.texto() == null ? "" : item.texto().trim());
            }
        }
        Map<Integer, ItemLoteResponse> porPosicao = new HashMap<>();
        if (textos.isEmpty()) {
            return porPosicao;
        }
        LoteSimilaridadeResponse analise = similaridadeService.avaliarLote(textos, 3, "CS");
        for (int k = 0; k < analise.itens().size(); k++) {
            porPosicao.put(posicoes.get(k), analise.itens().get(k));
        }
        return porPosicao;
    }

    private ItemVinculacaoResponse processarLinha(VinculacaoLoteRequest request, int linha,
                                                  ItemVinculacaoLote item, ItemLoteResponse analise,
                                                  String idempotencyKeyBase) {
        String texto = item.texto() == null ? "" : item.texto().trim();
        String csId = item.csId() == null ? "" : item.csId().trim();

        if (vazio(csId) && texto.isEmpty()) {
            return bloqueada(linha, "", "VAZIA", "Linha vazia — escolha um Código de Serviço.");
        }
        if (vazio(csId) && analise == null) {
            return bloqueada(linha, texto, "NAO_AVALIADA", "Item não avaliado — refaça a verificação.");
        }
        if (vazio(csId) && "BLOQUEADA".equals(analise.classificacao())) {
            return bloqueada(linha, texto, analise.situacao(), analise.motivo());
        }

        String classificacao;
        String situacao;
        String motivo;
        String acao = PreviewService.ACAO_CRIAR;
        if (!vazio(csId)) {
            classificacao = "DISPONIVEL";
            situacao = "SELECIONADO";
            motivo = "Código existente escolhido para reutilizar.";
        } else if ("DISPONIVEL".equals(analise.classificacao())) {
            classificacao = "DISPONIVEL";
            situacao = analise.situacao();
            motivo = analise.motivo();
            csId = analise.similar() == null ? "" : analise.similar().id();
        } else {
            classificacao = "CRIAR";
            situacao = analise.situacao();
            motivo = analise.motivo();
            if ("ALERTA".equals(situacao)) {
                // há parecidos: a decisão é humana — a Governança vê os matches e resolve
                acao = PreviewService.ACAO_ENVIAR_VALIDACAO;
                motivo = motivo + " Encaminhado para validação da Governança.";
            }
        }

        try {
            CodigoCompletoService.ResultadoCriacao resultado =
                    codigoCompletoService.criar(payload(request, csId, texto, acao),
                            chave(idempotencyKeyBase, linha));
            CodigoCompletoResponse r = resultado.resposta();
            return new ItemVinculacaoResponse(linha, texto.isEmpty() ? r.csId() : texto,
                    classificacao, situacao, motivo, r.csId(), r.idCompleto(), r.id(), r.status(),
                    resultado.requisicaoRepetida());
        } catch (Exception e) {
            String mensagem = e.getMessage() == null ? "Não foi possível vincular." : e.getMessage();
            return new ItemVinculacaoResponse(linha, texto.isEmpty() ? csId : texto,
                    "FALHA", "ERRO", mensagem, vazio(csId) ? null : csId, null, null, null, false);
        }
    }

    private static CriarCodigoCompletoRequest payload(VinculacaoLoteRequest request, String csId,
                                                      String texto, String acao) {
        boolean novo = vazio(csId);
        boolean validacao = PreviewService.ACAO_ENVIAR_VALIDACAO.equals(acao);
        return new CriarCodigoCompletoRequest(
                request.gsId(), null, request.cfId(), null, null,
                novo ? null : csId, novo ? texto : null,
                request.umId(), null, null,
                validacao ? JUSTIFICATIVA_LOTE : null,
                acao, request.usuarioCriacaoId());
    }

    private static ItemVinculacaoResponse bloqueada(int linha, String texto, String situacao, String motivo) {
        return new ItemVinculacaoResponse(linha, texto, "BLOQUEADA", situacao, motivo, null, null, null, null, false);
    }

    private static LoteVinculacaoResponse consolidar(List<ItemVinculacaoResponse> itens) {
        int disponiveis = 0;
        int criados = 0;
        int vinculados = 0;
        int aguardandoValidacao = 0;
        int bloqueadas = 0;
        int falhas = 0;
        for (ItemVinculacaoResponse item : itens) {
            boolean efetivado = item.status() != null && !item.requisicaoRepetida();
            switch (item.classificacao()) {
                case "DISPONIVEL" -> {
                    if (efetivado) disponiveis++;
                }
                case "CRIAR" -> {
                    if (efetivado) criados++;
                }
                case "BLOQUEADA" -> bloqueadas++;
                default -> falhas++;
            }
            if (efetivado) {
                if (CodigoCompleto.STATUS_ATIVO.equals(item.status())) vinculados++;
                if (CodigoCompleto.STATUS_PENDENTE.equals(item.status())) aguardandoValidacao++;
            }
        }
        return new LoteVinculacaoResponse(itens.size(), disponiveis, criados, vinculados,
                aguardandoValidacao, bloqueadas, falhas, itens);
    }

    private static String chave(String base, int linha) {
        return vazio(base) ? null : base + "-" + linha;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
