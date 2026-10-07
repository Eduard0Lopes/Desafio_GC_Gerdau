package br.com.gerdau.servicos.api.text;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextNormalizerTest {

    private final TextNormalizer normalizer = new TextNormalizer(Map.of(
            "manut", "manutencao", "mec", "mecanica", "norm", "normal", "m2", "metro quadrado"));

    @Test
    void removeAcentosCaixaEPontuacao() {
        assertEquals("manutencao mecanica normal", normalizer.normalize("Manutenção, MECÂNICA - normal."));
    }

    @Test
    void removePalavrasVaziasComoNoExemploDoDocumento() {
        assertEquals("fornecedor manutencao industrial", normalizer.normalize("Fornecedor de manutenção industrial"));
    }

    @Test
    void expandeAbreviacoesConhecidas() {
        assertEquals("mecanica manutencao normal", normalizer.normalize("MEC.MANUT.NORM"));
        assertEquals("area metro quadrado", normalizer.normalize("Área m²"));
    }

    @Test
    void ehIdempotente() {
        String once = normalizer.normalize("Mec manut norm de hr");
        assertEquals(once, normalizer.normalize(once));
    }

    @Test
    void preservaNumeros() {
        assertEquals("cod 4521 mecanica", normalizer.normalize("COD 4521 - MEC"));
    }

    @Test
    void tratavaziosENulos() {
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize("   "));
        assertEquals("", normalizer.normalize("---"));
    }

    @Test
    void prefixoNaoExpandeAbreviacao() {
        // digitar "mec" deve casar mecânica E mecânico
        assertEquals(List.of("mec"), normalizer.prefixTokens("Mec"));
    }

    @Test
    void soPalavrasVaziasMantemTodas() {
        assertTrue(normalizer.contentTokens("de da").size() == 2);
    }
}
