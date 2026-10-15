package com.gerdau.desafio.search;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;

import java.util.List;

/**
 * Abstração do mecanismo de busca/similaridade (documento técnico, seções 4.2 e 5.4):
 * a API não depende de um motor específico — hoje o motor local (Java) ou o motor
 * do Time de IA (Python); amanhã Atlas Search/OpenSearch, sem mudar contratos.
 *
 * A mesma regra anti-duplicidade vale para todos os componentes do catálogo
 * (G.S., C.F., C.S. e U.M.).
 */
public interface SearchClient {

    String TIPO_CS = "CS";
    String TIPO_GS = "GS";
    String TIPO_CF = "CF";
    String TIPO_UM = "UM";

    /** Identificação do mecanismo em uso ("local" | "python"). */
    String mecanismo();

    /** Candidatos mais semelhantes à consulta, em ordem reproduzível de score. */
    List<CandidatoSimilarResponse> buscarSimilares(String consulta, int limite, double scoreMinimo, String tipo);

    /** Invalida caches internos (após criar/atualizar itens de catálogo). */
    void invalidarCache();
}
