package br.com.gerdau.servicos.api.dto;

import br.com.gerdau.servicos.api.domain.TipoComponente;

public final class ComponenteDtos {

    private ComponenteDtos() {}

    /** Resposta enxuta de busca (doc. 14.1): id, texto, score. "rotulo" é o texto pronto para exibir. */
    public record ComponenteDto(String id, TipoComponente tipo, String texto, String sigla,
                                String rotulo, Double score, boolean aproximado) {}
}
