package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.request.VinculacaoLoteRequest;
import com.gerdau.desafio.dto.request.VinculacaoLoteRequest.ItemVinculacaoLote;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.ItemLoteResponse;
import com.gerdau.desafio.dto.response.ItemVinculacaoResponse;
import com.gerdau.desafio.dto.response.LoteSimilaridadeResponse;
import com.gerdau.desafio.dto.response.LoteVinculacaoResponse;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VinculacaoLoteService {

    private static final Logger log = LoggerFactory.getLogger(VinculacaoLoteService.class);

    static final String JUSTIFICATIVA_LOTE =
            "Vinculação em massa — item com similaridade alertada; encaminhado para validação da Governança.";

    static final String DISPONIVEL = "DISPONIVEL";
    static final String CRIAR = "CRIAR";
    static final String BLOQUEADA = "BLOQUEADA";
    static final String FALHA = "FALHA";

    static final String ALERTAR = "ALERTAR";

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
        log.info("[VINCULACAO/Lote] total={} reutilizados={} criados={} vinculados={} aguardandoValidacao={}",
                resposta.total(), resposta.reutilizados(), resposta.criados(),
                resposta.vinculados(), resposta.aguardandoValidacao());
        return resposta;
    }

    private Map<Integer, ItemLoteResponse> avaliarNovos(List<ItemVinculacaoLote> itens) {
        List<String> textos = new ArrayList<>();
        Map<String, Integer> posicaoDoTexto = new HashMap<>();
        Map<Integer, Integer> avaliacaoDaLinha = new HashMap<>();
        for (int i = 0; i < itens.size(); i++) {
            ItemVinculacaoLote item = itens.get(i);
            String texto = item.texto() == null ? "" : item.texto().trim();
            if (!TextoUtil.vazio(item.csId()) || texto.isEmpty()) {
                continue;
            }
            String chave = TextoUtil.normalizar(texto);
            Integer posicao = posicaoDoTexto.get(chave);
            if (posicao == null) {
                posicao = textos.size();
                posicaoDoTexto.put(chave, posicao);
                textos.add(texto);
            }
            avaliacaoDaLinha.put(i, posicao);
        }
        if (textos.isEmpty()) {
            return Map.of();
        }
        LoteSimilaridadeResponse analise = similaridadeService.avaliarLote(textos, 3, "CS");
        Map<Integer, ItemLoteResponse> porLinha = new HashMap<>();
        for (Map.Entry<Integer, Integer> entrada : avaliacaoDaLinha.entrySet()) {
            porLinha.put(entrada.getKey(), analise.itens().get(entrada.getValue()));
        }
        return porLinha;
    }

    private ItemVinculacaoResponse processarLinha(VinculacaoLoteRequest request, int linha,
                                                  ItemVinculacaoLote item, ItemLoteResponse analise,
                                                  String idempotencyKeyBase) {
        String texto = item.texto() == null ? "" : item.texto().trim();
        String csId = item.csId() == null ? "" : item.csId().trim();

        if (TextoUtil.vazio(csId) && texto.isEmpty()) {
            return bloqueada(linha, "", "Linha vazia — escolha um Código de Serviço.");
        }
        if (TextoUtil.vazio(csId) && analise == null) {
            return bloqueada(linha, texto, "Item não avaliado — refaça a verificação.");
        }
        if (TextoUtil.vazio(csId) && BLOQUEADA.equals(analise.classificacao())) {
            return bloqueada(linha, texto, analise.motivo());
        }

        String classificacao;
        String decisao;
        String motivo;
        String acao = PreviewService.ACAO_CRIAR;
        if (!TextoUtil.vazio(csId)) {
            classificacao = DISPONIVEL;
            decisao = null;
            motivo = "Código existente escolhido para reutilizar.";
        } else if (DISPONIVEL.equals(analise.classificacao())) {
            classificacao = DISPONIVEL;
            decisao = analise.decisao();
            motivo = analise.motivo();
            csId = analise.similar() == null ? "" : analise.similar().id();
        } else {
            classificacao = CRIAR;
            decisao = analise.decisao();
            motivo = analise.motivo();
            if (ALERTAR.equals(decisao)) {
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
                    classificacao, decisao, motivo, r.csId(), r.idCompleto(),
                    resultado.requisicaoRepetida());
        } catch (Exception e) {
            String mensagem = e.getMessage() == null ? "Não foi possível vincular." : e.getMessage();
            return new ItemVinculacaoResponse(linha, texto.isEmpty() ? csId : texto,
                    FALHA, null, mensagem, TextoUtil.vazio(csId) ? null : csId, null, false);
        }
    }

    private static CriarCodigoCompletoRequest payload(VinculacaoLoteRequest request, String csId,
                                                      String texto, String acao) {
        boolean novo = TextoUtil.vazio(csId);
        boolean validacao = PreviewService.ACAO_ENVIAR_VALIDACAO.equals(acao);
        return new CriarCodigoCompletoRequest(
                request.gsId(), null, request.cfId(), null, null,
                novo ? null : csId, novo ? texto : null,
                request.umId(), null, null,
                validacao ? JUSTIFICATIVA_LOTE : null,
                acao, request.usuarioCriacaoId());
    }

    private static ItemVinculacaoResponse bloqueada(int linha, String texto, String motivo) {
        return new ItemVinculacaoResponse(linha, texto, BLOQUEADA, null, motivo, null, null, false);
    }

    private static LoteVinculacaoResponse consolidar(List<ItemVinculacaoResponse> itens) {
        int reutilizados = 0;
        int criados = 0;
        int vinculados = 0;
        int aguardandoValidacao = 0;
        for (ItemVinculacaoResponse item : itens) {
            if (item.requisicaoRepetida() || item.idCompleto() == null) {
                continue;
            }
            if (DISPONIVEL.equals(item.classificacao())) {
                reutilizados++;
            } else {
                criados++;
            }
            if (CRIAR.equals(item.classificacao()) && ALERTAR.equals(item.decisao())) {
                aguardandoValidacao++;
            } else {
                vinculados++;
            }
        }
        return new LoteVinculacaoResponse(itens.size(), reutilizados, criados,
                vinculados, aguardandoValidacao, itens);
    }

    private static String chave(String base, int linha) {
        return TextoUtil.vazio(base) ? null : base + "-" + linha;
    }
}
