package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Componente C.F. — documento técnico, seção 7.2.
 * Identidade oficial = código do fornecedor no SAP (ex.: 200982674).
 */
@Data
@Document(collection = "codigos_fornecedor")
public class CodigoFornecedor {

    @Id
    private String id;

    @Indexed
    @Field("codigoFornecedorSAP")
    private Long codigoFornecedorSAP;

    @Field("nome")
    private String nome;

    @Indexed
    @Field("nomeNormalizado")
    private String nomeNormalizado;

    @Indexed
    @Field("origem")
    private String origem;

    @Field("active")
    private Boolean active = true;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;
}
