package br.com.gerdau.servicos.api.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortugueseStemmerTest {

    private final PortugueseStemmer stemmer = new PortugueseStemmer();

    @Test
    void aproximaFlexoesComuns() {
        assertEquals(stemmer.stem("mecanico"), stemmer.stem("mecanica"));
        assertEquals(stemmer.stem("manutencao"), stemmer.stem("manutencoes"));
        assertEquals(stemmer.stem("servico"), stemmer.stem("servicos"));
        assertEquals(stemmer.stem("instalador"), stemmer.stem("instaladores"));
    }

    @Test
    void preservaNumerosEPalavrasCurtas() {
        assertEquals("4521", stemmer.stem("4521"));
        assertEquals("gas", stemmer.stem("gas"));
    }
}
