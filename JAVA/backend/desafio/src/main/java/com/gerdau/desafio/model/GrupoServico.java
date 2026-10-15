package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Componente G.S. — documento técnico, seção 7.1.
 * Identidade oficial derivada do nome (ex.: "CIVIL").
 * Campos-base seguem a especificação (seção 5): active, createdAt, updatedAt.
 */
@Data
@Document(collection = "grupos_servico")
public class GrupoServico {

    @Id
    private String id;

    @Field("nome")
    private String nome;

    @Indexed
    @Field("nomeNormalizado")
    private String nomeNormalizado;

    @Indexed
    @Field("origem")
    private String origem;

    /** Ativo por padrão; desativar em vez de apagar preserva referências (seção 5). */
    @Field("active")
    private Boolean active = true;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;
}
