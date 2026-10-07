package br.com.gerdau.servicos.api.dto;

import br.com.gerdau.servicos.api.domain.DecisaoSimilaridade;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.ComponenteDtos.ComponenteDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ItemDtos {

    private ItemDtos() {}

    /**
     * "texto" = nome (GS, CF, UM) ou descrição (CS, máx. 40 caracteres).
     * "sigla" só para UM. "justificativa" é exigida na faixa de ALERTA.
     */
    public record CriarItemRequest(@NotNull TipoComponente tipo,
                                   @Size(max = 200) String texto,
                                   @Size(max = 10) String sigla,
                                   @Size(max = 500) String justificativa) {}

    public record ItemCriadoResponse(ComponenteDto item, DecisaoSimilaridade decisaoSimilaridade,
                                     boolean excecaoRegistrada) {}
}
