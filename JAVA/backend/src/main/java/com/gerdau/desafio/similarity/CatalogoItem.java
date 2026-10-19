package com.gerdau.desafio.similarity;

import java.util.List;
import java.util.Set;

public record CatalogoItem(
        String id,
        String codigoOriginal,
        List<String> legacyIds,
        String descricao,
        String descricaoNormalizada,
        Set<String> tokens,
        List<String> numeros,
        boolean casoIsolado,
        String motivoCasoIsolado
) {
}
