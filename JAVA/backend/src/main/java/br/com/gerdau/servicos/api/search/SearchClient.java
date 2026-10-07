package br.com.gerdau.servicos.api.search;

import br.com.gerdau.servicos.api.domain.TipoComponente;

import java.util.List;

/**
 * Abstração do mecanismo de busca textual (doc. seções 4.2 e 5.4). Trocar Atlas Search por
 * OpenSearch/Elasticsearch = criar outra implementação; nada mais muda na aplicação.
 */
public interface SearchClient {

    /** Busca enquanto o usuário digita (prefixo + tolerância a erro de digitação). */
    List<SearchHit> autocomplete(TipoComponente tipo, List<String> prefixTokens, int limit);

    /** Candidatos para cálculo de similaridade (mais amplo; a nota é calculada depois). */
    List<SearchHit> candidates(TipoComponente tipo, List<String> contentTokens, int limit);
}
