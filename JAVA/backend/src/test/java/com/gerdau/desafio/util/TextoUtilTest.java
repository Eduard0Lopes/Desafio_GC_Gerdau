package com.gerdau.desafio.util;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextoUtilTest {

    @Test
    void palavrasSemNumerosTratamNumeroColadoEComEspacoIgual() {
        assertEquals(TextoUtil.palavrasDoNucleoSemNumeros("telhado 2"),
                TextoUtil.palavrasDoNucleoSemNumeros("telhado2"));
        assertEquals(TextoUtil.palavrasDoNucleoSemNumeros("telhados"),
                TextoUtil.palavrasDoNucleoSemNumeros("telhado 2"));
        assertEquals(TextoUtil.palavrasDoNucleoSemNumeros("Pintura m2"),
                TextoUtil.palavrasDoNucleoSemNumeros("pinturas"));
    }

    @Test
    void palavrasSemNumerosColapsamLetrasRepetidas() {
        assertEquals(TextoUtil.palavrasDoNucleoSemNumeros("telhadosss"),
                TextoUtil.palavrasDoNucleoSemNumeros("telhado"));
    }

    @Test
    void singularizaPluraisComuns() {
        assertEquals("telhado", TextoUtil.singularizar("telhados"));
        assertEquals("telhado", TextoUtil.singularizar("telhado"));
        assertEquals("caixa", TextoUtil.singularizar("caixas"));
        assertEquals("fase", TextoUtil.singularizar("fases"));
        assertEquals("solucao", TextoUtil.singularizar("solucoes"));
        assertEquals("acao", TextoUtil.singularizar("acoes"));
        assertEquals("bem", TextoUtil.singularizar("bens"));
        assertEquals("jardim", TextoUtil.singularizar("jardins"));
        assertEquals("luz", TextoUtil.singularizar("luzes"));
        assertEquals("papel", TextoUtil.singularizar("papeis"));
        assertEquals("material", TextoUtil.singularizar("materiais"));
        assertEquals("civil", TextoUtil.singularizar("civis"));
        assertEquals("trator", TextoUtil.singularizar("tratores"));
    }

    @Test
    void singularizaDesconsiderandoAcentos() {
        assertEquals("solucao", TextoUtil.singularizar("Soluções"));
        assertEquals("papel", TextoUtil.singularizar("papéis"));
    }

    @Test
    void naoSingularizaCodigosCurtosOuComDigitos() {
        assertEquals("4521", TextoUtil.singularizar("4521"));
        assertEquals("1/2", TextoUtil.singularizar("1/2"));
        assertEquals("sv", TextoUtil.singularizar("sv"));
        assertEquals("eixo", TextoUtil.singularizar("eixo"));
    }

    @Test
    void variantesIncluemFormaDigitadaSingularEPlural() {
        Set<String> formas = TextoUtil.variantesPalavra("telhados");
        assertTrue(formas.contains("telhados"));
        assertTrue(formas.contains("telhado"));
        assertTrue(TextoUtil.variantesPalavra("telhado").contains("telhados"));
        assertTrue(TextoUtil.variantesPalavra("Soluções").contains("solucao"));
        assertTrue(TextoUtil.variantesPalavra("Soluções").contains("solucoes"));
    }

    @Test
    void padraoBuscaCasaPluralESingularNosDoisSentidos() {
        Pattern consultaPlural = Pattern.compile(TextoUtil.padraoBusca("telhados"));
        Pattern consultaSingular = Pattern.compile(TextoUtil.padraoBusca("telhado"));

        assertTrue(consultaPlural.matcher("Cobertura de Telhado galvanizado").find());
        assertTrue(consultaPlural.matcher("TELHADOS industriais").find());
        assertTrue(consultaSingular.matcher("telhados metalicos").find());
        assertTrue(consultaSingular.matcher("reposicao de telhado").find());
    }

    @Test
    void padraoBuscaCasaPalavrasComAcentosAusentes() {
        Pattern consulta = Pattern.compile(TextoUtil.padraoBusca("solucoes"));
        assertTrue(consulta.matcher("registro de solucao quimica").find());
        assertTrue(consulta.matcher("solucoes industriais").find());

        Pattern comAcento = Pattern.compile(TextoUtil.padraoBusca("Soluções"));
        assertTrue(comAcento.matcher("Soluções industriais").find());
    }

    @Test
    void padraoBuscaMantemCodigosLiterais() {
        Pattern codigo = Pattern.compile(TextoUtil.padraoBusca("1/2"));
        assertTrue(codigo.matcher("SV ELETRODUTO PVC 1/2").find());
        assertFalse(codigo.matcher("SV ELETRODUTO PVC 3/4").find());
    }

    @Test
    void padraoBuscaDeVariasPalavrasAceitaQualquerOrdem() {
        Pattern consulta = Pattern.compile(TextoUtil.padraoBusca("telhado metalico"));
        assertTrue(consulta.matcher("Telhados  metalicos galvanizados").find());
        assertTrue(consulta.matcher("metalico de telhado").find());
        assertFalse(consulta.matcher("telhado pintado").find(), "todas as palavras precisam aparecer");
    }
}
