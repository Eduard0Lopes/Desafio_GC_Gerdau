package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.dto.response.SimilaridadeResponse;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.search.SearchClient;
import com.gerdau.desafio.similarity.RankingPolicy;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta de itens semelhantes a partir de uma descrição (seções 6 e 10.1).
 * A busca é delegada ao {@link SearchClient} — a mesma regra vale para
 * G.S., C.F., C.S. e U.M.
 */
@Service
public class SimilaridadeService {

    private static final Logger log = LoggerFactory.getLogger(SimilaridadeService.class);

    static final int LIMITE_PADRAO = 5;
    static final int LIMITE_MAXIMO = 20;
    static final int TAMANHO_MAXIMO_CONSULTA = 200;

    private final SearchClient searchClient;
    private final RankingPolicy policy;
    private final double scoreMinimo;

    public SimilaridadeService(SearchClient searchClient,
                               RankingPolicy policy,
                               @Value("${similaridade.score-minimo:0.30}") double scoreMinimo) {
        this.searchClient = searchClient;
        this.policy = policy;
        this.scoreMinimo = scoreMinimo;
    }

    public SimilaridadeResponse consultar(String descricao, Integer limite, String tipo) {
        if (descricao == null || descricao.isBlank()) {
            throw new BusinessException("A descrição é obrigatória.");
        }
        if (descricao.length() > TAMANHO_MAXIMO_CONSULTA) {
            throw new BusinessException("A descrição de consulta não pode exceder " + TAMANHO_MAXIMO_CONSULTA + " caracteres.");
        }
        String normalizada = TextoUtil.normalizar(descricao);
        if (normalizada.isEmpty()) {
            throw new BusinessException("A descrição não tem texto pesquisável (use letras ou números).");
        }
        int limiteEfetivo = limite == null ? LIMITE_PADRAO : limite;
        if (limiteEfetivo < 1 || limiteEfetivo > LIMITE_MAXIMO) {
            throw new BusinessException("O limite deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }

        long inicio = System.nanoTime();
        List<CandidatoSimilarResponse> candidatos =
                searchClient.buscarSimilares(descricao, limiteEfetivo, scoreMinimo, tipo);

        double melhorScore = candidatos.isEmpty() ? 0.0 : candidatos.get(0).score();
        RankingPolicy.Decisao decisao = policy.aplicar(melhorScore);

        Map<String, Double> limiares = new LinkedHashMap<>();
        limiares.put("duplicidade", policy.getLimiarDuplicidade());
        limiares.put("alerta", policy.getLimiarAlerta());

        log.info("[SIMILARIDADE] mecanismo={} tipo={} consulta='{}' candidatos={} melhorScore={} decisao={} em {} ms",
                searchClient.mecanismo(), tipo == null ? "CS" : tipo, normalizada,
                candidatos.size(), melhorScore, decisao.decisao(), (System.nanoTime() - inicio) / 1_000_000);

        return new SimilaridadeResponse(
                descricao.trim(),
                normalizada,
                descricao.trim().length(),
                TextoUtil.excedeLimiteSap(descricao.trim()),
                decisao.status(),
                decisao.decisao(),
                decisao.acaoRecomendada(),
                limiares,
                candidatos.size(),
                candidatos);
    }

    public void invalidarCache() {
        searchClient.invalidarCache();
    }
}
