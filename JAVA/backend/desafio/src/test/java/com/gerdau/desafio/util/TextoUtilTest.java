package com.gerdau.desafio.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Mesmos casos de IA/tests/test_normalizacao.py. */
class TextoUtilTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "SV-EQUIPE TELHADO|sv equipe telhado",
            "Manutenção  Mecânica!!|manutencao mecanica",
            "'  Pavimentação/Segurança (NR-10) '|pavimentacao seguranca nr 10",
            "Centímetro cúbico|centimetro cubico",
            "''|''"
    })
    void normalizaIgualAoPython(String entrada, String esperado) {
        assertThat(TextoUtil.normalizar(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource(value = {"'  a   b '"})
    void limparColapsaEspacos(String entrada) {
        assertThat(TextoUtil.limpar(entrada)).isEqualTo("a b");
        assertThat(TextoUtil.normalizar(null)).isEmpty();
    }
}
