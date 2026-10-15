package com.gerdau.desafio.model;

import org.springframework.data.mongodb.core.mapping.Field;

/** Instantâneo de um item semelhante encontrado na análise (seções 10.2 e 12.3). */
public record RegistroSimilar(

        @Field("posicao")
        int posicao,

        @Field("id")
        String id,

        @Field("codigoOriginal")
        String codigoOriginal,

        @Field("descricao")
        String descricao,

        @Field("score")
        double score,

        @Field("motivo")
        String motivo
) {
}
