package br.com.gerdau.servicos.api.text;

import br.com.gerdau.servicos.api.domain.DecisaoSimilaridade;
import br.com.gerdau.servicos.api.domain.SimilarityPolicy;
import br.com.gerdau.servicos.api.text.SimilarityScorer.Motivo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarityScorerTest {

    private final TextNormalizer normalizer = new TextNormalizer(Map.of(
            "manut", "manutencao", "mec", "mecanica", "norm", "normal"));
    private final SimilarityPolicy policy = new SimilarityPolicy(0.75, 0.90);
    private final SimilarityScorer scorer = new SimilarityScorer(normalizer, policy.blockThreshold() - 0.01);

    private SimilarityScorer.Resultado score(String a, String b) {
        return scorer.score(normalizer.normalize(a), normalizer.normalize(b));
    }

    @Test
    void textoIdenticoAposNormalizacaoVale1() {
        var r = score("Manutenção Mecânica Normal", "manutencao mecanica normal");
        assertEquals(1.0, r.score());
        assertEquals(Motivo.IDENTICO, r.motivo());
        assertEquals(DecisaoSimilaridade.REVISAR, policy.decide(r.score()));
    }

    @Test
    void mesmasPalavrasEmOrdemDiferenteBloqueiam() {
        var r = score("mecânico manutenção", "manutenção mecânica");
        assertTrue(r.score() >= 0.90);
        assertEquals(DecisaoSimilaridade.REVISAR, policy.decide(r.score()));
    }

    @Test
    void exemploDoDocumentoGeraAlerta() {
        var r = score("MECANICO MANUTENCAO HORA NORMAL", "Manutenção mecânica normal");
        assertEquals(DecisaoSimilaridade.ALERTAR, policy.decide(r.score()));
    }

    @Test
    void erroDeDigitacaoNaoPassaDespercebido() {
        var r = score("manutencao mecanica", "manutencao mecnica");
        assertTrue(r.score() >= 0.75);
    }

    @Test
    void itensDiferentesSaoPermitidos() {
        assertEquals(DecisaoSimilaridade.PERMITIR, policy.decide(score("Limpeza predial", "Pintura de paredes").score()));
        assertEquals(DecisaoSimilaridade.PERMITIR,
                policy.decide(score("Eletricista instalador", "Mecânico manutenção com especialização").score()));
    }

    @Test
    void numerosDiferentesNuncaBloqueiamAutomaticamente() {
        var r = score("Mecânico 2 pessoas", "Mecânico 3 pessoas");
        assertEquals(DecisaoSimilaridade.ALERTAR, policy.decide(r.score()));
    }

    @Test
    void vazioNaoTemRelacao() {
        assertEquals(0.0, scorer.score("", "x").score());
    }
}
