package com.gerdau.desafio.service.similaridade;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Garante paridade com o motor Python usando a fixture compartilhada
 * IA/tests/fixtures/similaridade_casos.json (gerada pelo fuzzy_engine.py).
 */
class FuzzyEngineTest {

    private static final Path FIXTURE = Path.of("../../../IA/tests/fixtures/similaridade_casos.json");
    private static final Pattern CASO = Pattern.compile(
            "\"a\":\\s*\"([^\"]*)\",\\s*\"b\":\\s*\"([^\"]*)\",\\s*\"score\":\\s*([0-9.eE-]+),\\s*\"status\":\\s*\"(\\w+)\"");

    record Caso(String a, String b, double score, StatusSimilaridade status) {
    }

    @TestFactory
    Stream<DynamicTest> mesmoScoreQueOPython() throws Exception {
        Matcher m = CASO.matcher(Files.readString(FIXTURE));
        List<Caso> casos = m.results()
                .map(r -> new Caso(r.group(1), r.group(2), Double.parseDouble(r.group(3)),
                        StatusSimilaridade.valueOf(r.group(4))))
                .toList();
        assertThat(casos).isNotEmpty();

        return casos.stream().map(caso -> DynamicTest.dynamicTest(caso.a() + " | " + caso.b(), () -> {
            double score = FuzzyEngine.similaridade(caso.a(), caso.b());
            assertThat(score).isCloseTo(caso.score(), within(1e-12));
            assertThat(RankingPolicy.classificar(score)).isEqualTo(caso.status());
        }));
    }

    @Test
    void razaoSequenciaComoDifflib() {
        assertThat(FuzzyEngine.razaoSequencia("abcd", "bcde")).isEqualTo(0.75);
        assertThat(FuzzyEngine.razaoSequencia("", "")).isEqualTo(1.0);
        assertThat(FuzzyEngine.similaridade("", "abc")).isZero();
    }

    @Test
    void limiaresDaPolitica() {
        assertThat(RankingPolicy.classificar(0.88)).isEqualTo(StatusSimilaridade.DUPLICIDADE);
        assertThat(RankingPolicy.classificar(0.8799)).isEqualTo(StatusSimilaridade.ALERTA);
        assertThat(RankingPolicy.classificar(0.70)).isEqualTo(StatusSimilaridade.ALERTA);
        assertThat(RankingPolicy.classificar(0.6999)).isEqualTo(StatusSimilaridade.ORIGINAL);
    }
}
