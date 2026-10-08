package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "unidades_medida")
public class UnidadeMedida {

    @Id
    private String id;

    @Field("sigla")
    private String sigla;

    @Field("nomeNormalizado")
    private String nomeNormalizado;

    @Field("origem")
    private String origem;

    @Field("codigoOriginal")
    private String codigoOriginal;
}