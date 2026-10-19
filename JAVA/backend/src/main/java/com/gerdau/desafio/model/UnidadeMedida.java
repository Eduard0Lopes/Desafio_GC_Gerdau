package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Document(collection = "unidades_medida")
public class UnidadeMedida {

    @Id
    private String id;

    @Indexed
    @Field("sigla")
    private String sigla;

    @Field("nome")
    private String nome;

    @Indexed
    @Field("nomeNormalizado")
    private String nomeNormalizado;

    @Indexed
    @Field("origem")
    private String origem;

    @Indexed
    @Field("status")
    private String status = StatusComponente.ATIVO;

    @Field("active")
    private Boolean active = true;

    @Field("criadoPor")
    private String criadoPor;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;
}
