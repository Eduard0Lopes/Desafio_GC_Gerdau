package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.dto.response.ItemLoteResponse;
import com.gerdau.desafio.dto.response.LoteSimilaridadeResponse;
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

@Service
public class SimilaridadeService {

    private static final Logger log = LoggerFactory.getLogger(SimilaridadeService.class);

    static final int LIMITE_PADRAO = 5;
    static final int LIMITE_MAXIMO = 20;
    static final int TAMANHO_MAXIMO_CONSULTA = 200;
    static final int LIMITE_MAXIMO_LOTE = 300;

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

    /**
     * Avaliação em lote para a criação em massa de códigos: para cada linha informada,
     * diz se o código já está DISPONÍVEL na base (é reutilizar), se PRECISA SER CRIADO
     * ou se está BLOQUEADA (linha vazia, repetida na lista ou acima do limite SAP).
     *
     * As regras de negócio valem para a lista inteira: nada duplicado entra duas vezes
     * e um item com duplicidade provável (score ≥ limiar de duplicidade) conta como
     * "disponível" — o código já existe para esta necessidade.
     */
    public LoteSimilaridadeResponse avaliarLote(List<String> textos, Integer limite, String tipo) {
        if (textos == null || textos.isEmpty()) {
            throw new BusinessException("Informe ao menos um código para avaliar.");
        }
        if (textos.size() > LIMITE_MAXIMO_LOTE) {
            throw new BusinessException("Avalie no máximo " + LIMITE_MAXIMO_LOTE + " códigos por vez.");
        }
        String tipoEfetivo = (tipo == null || tipo.isBlank()) ? "CS" : tipo.trim().toUpperCase(java.util.Locale.ROOT);
        int limiteEfetivo = limite == null ? LIMITE_PADRAO : limite;
        if (limiteEfetivo < 1 || limiteEfetivo > LIMITE_MAXIMO) {
            throw new BusinessException("O limite deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }

        List<ItemLoteResponse> itens = new java.util.ArrayList<>();
        java.util.Set<String> jaInformados = new java.util.HashSet<>();
        int disponiveis = 0;
        int aCriar = 0;
        int bloqueadas = 0;
        int linha = 0;
        for (String texto : textos) {
            linha++;
            ItemLoteResponse item = avaliarLinha(linha, texto, limiteEfetivo, tipoEfetivo, jaInformados);
            itens.add(item);
            switch (item.classificacao()) {
                case "DISPONIVEL" -> disponiveis++;
                case "CRIAR" -> aCriar++;
                default -> bloqueadas++;
            }
        }
        log.info("[SIMILARIDADE/Lote] tipo={} total={} disponiveis={} aCriar={} bloqueadas={}",
                tipoEfetivo, itens.size(), disponiveis, aCriar, bloqueadas);
        return new LoteSimilaridadeResponse(tipoEfetivo, itens.size(), disponiveis, aCriar, bloqueadas, itens);
    }

    private ItemLoteResponse avaliarLinha(int linha, String texto, int limite, String tipo,
                                          java.util.Set<String> jaInformados) {
        String bruto = texto == null ? "" : texto.trim();
        if (bruto.isEmpty()) {
            return new ItemLoteResponse(linha, "", "BLOQUEADA", "VAZIA",
                    "Linha vazia — informe o código.", null, 0.0, null);
        }
        if (TextoUtil.excedeLimiteSap(bruto)) {
            return new ItemLoteResponse(linha, bruto, "BLOQUEADA", "LIMITE_SAP",
                    "Descrição com " + bruto.length() + " caracteres — o limite SAP é "
                            + TextoUtil.LIMITE_SAP + ".", null, 0.0, null);
        }
        String normalizada = TextoUtil.normalizar(bruto);
        if (normalizada.isEmpty()) {
            return new ItemLoteResponse(linha, bruto, "BLOQUEADA", "VAZIA",
                    "Sem texto pesquisável (use letras ou números).", null, 0.0, null);
        }
        if (!jaInformados.add(normalizada)) {
            return new ItemLoteResponse(linha, bruto, "BLOQUEADA", "REPETIDA",
                    "Já informada nesta lista — cada código entra uma vez só.", null, 0.0, null);
        }

        SimilaridadeResponse consulta = consultar(bruto, limite, tipo);
        List<CandidatoSimilarResponse> candidatos = consulta.candidatos();
        CandidatoSimilarResponse melhor = candidatos.isEmpty() ? null : candidatos.get(0);
        double melhorScore = melhor == null ? 0.0 : melhor.score();

        if (melhor != null && (mesmoItem(normalizada, melhor, tipo) || melhorScore >= limiarDuplicidade())) {
            boolean jaExiste = mesmoItem(normalizada, melhor, tipo);
            return new ItemLoteResponse(linha, bruto, "DISPONIVEL", jaExiste ? "JA_EXISTE" : "PARECIDO",
                    jaExiste
                            ? "Já existe na base — reutilize " + melhor.id() + "."
                            : "Duplicidade provável de " + melhor.id() + " (" + percentual(melhorScore)
                                    + ") — reutilize em vez de criar.",
                    consulta.decisao(), melhorScore, melhor);
        }
        if ("ALERTAR".equals(consulta.decisao())) {
            return new ItemLoteResponse(linha, bruto, "CRIAR", "ALERTA",
                    "Precisa ser criado — há parecidos (" + percentual(melhorScore)
                            + " com " + melhor.id() + "); confira antes de confirmar.",
                    consulta.decisao(), melhorScore, melhor);
        }
        return new ItemLoteResponse(linha, bruto, "CRIAR", "OK",
                "Precisa ser criado — sem itens semelhantes na base.",
                consulta.decisao(), melhorScore, melhor);
    }

    /** Mesmo item: descrição idêntica (normalizada) ou sigla de UM seguida do nome. */
    private static boolean mesmoItem(String normalizada, CandidatoSimilarResponse candidato, String tipo) {
        String alvo = TextoUtil.normalizar(candidato.descricao() == null ? "" : candidato.descricao());
        if (alvo.equals(normalizada)) {
            return true;
        }
        return "UM".equalsIgnoreCase(tipo) && alvo.startsWith(normalizada + " - ");
    }

    private double limiarDuplicidade() {
        return policy.getLimiarDuplicidade();
    }

    private static String percentual(double score) {
        return Math.round(score * 100) + "%";
    }
}
