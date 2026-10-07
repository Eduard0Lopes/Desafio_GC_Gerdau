package br.com.gerdau.servicos.api.dto;

import br.com.gerdau.servicos.api.domain.DecisaoSimilaridade;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class SimilarityDtos {

    private SimilarityDtos() {}

    public record SimilaridadeRequest(@NotNull TipoComponente tipo,
                                      @NotBlank @Size(max = 200) String texto) {}

    public record SimilarItem(String id, String texto, String sigla, double score, String motivo) {}

    /**
     * "decisao" é a decisão calculada pela política; "enforcement" informa se ela BLOQUEIA (ENFORCE)
     * ou apenas é registrada (OBSERVE — modo de observação, doc. 12.4).
     */
    public record SimilaridadeResponse(DecisaoSimilaridade decisao, double scoreMaximo, String enforcement,
                                       double limiarAlerta, double limiarBloqueio, boolean aproximado,
                                       List<SimilarItem> similares) {}
}
