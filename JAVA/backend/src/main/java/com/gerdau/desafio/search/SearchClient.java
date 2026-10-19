package com.gerdau.desafio.search;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;

import java.util.List;

public interface SearchClient {

    String TIPO_CS = "CS";
    String TIPO_GS = "GS";
    String TIPO_CF = "CF";
    String TIPO_UM = "UM";

    String mecanismo();

    List<CandidatoSimilarResponse> buscarSimilares(String consulta, int limite, double scoreMinimo, String tipo);

    void invalidarCache();
}
