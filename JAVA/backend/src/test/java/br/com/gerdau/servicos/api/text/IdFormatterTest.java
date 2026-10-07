package br.com.gerdau.servicos.api.text;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdFormatterTest {

    private final IdFormatter ids = new IdFormatter(3, 3, 6, 2);

    @Test
    void montaOIdCompletoDe14PosicoesComoNoDocumento() {
        assertEquals("00100100000101", ids.completeId("001", "001", "000001", "01"));
    }

    @Test
    void completaComZerosIdsNumericosCurtos() {
        assertEquals("00100100000101", ids.completeId("1", "1", "1", "1"));
    }

    @Test
    void formataSequencia() {
        assertEquals("000007", ids.format(TipoComponente.CS, 7));
    }

    @Test
    void falhaQuandoASequenciaEstoura() {
        assertThrows(IllegalStateException.class, () -> ids.format(TipoComponente.UM, 100));
    }

    @Test
    void rejeitaIdMaisLargoQueOPadrao() {
        assertThrows(IllegalArgumentException.class, () -> ids.canonical(TipoComponente.GS, "1234"));
    }

    @Test
    void rejeitaAlfanumericoForaDaLargura() {
        assertThrows(IllegalArgumentException.class, () -> ids.canonical(TipoComponente.GS, "AB"));
        assertEquals("ABC", ids.canonical(TipoComponente.GS, "ABC"));
    }
}
