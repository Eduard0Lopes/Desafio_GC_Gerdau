package com.gerdau.desafio.similarity;

import java.util.List;
import java.util.Set;

/** Item do catálogo já preparado para comparação (texto normalizado e tokens calculados uma única vez). */
public record CatalogoItem(
        String id,
        String codigoOriginal,
        List<String> legacyIds,
        String descricao,
        String descricaoNormalizada,
        Set<String> tokens,
        List<String> numeros
) {
}
