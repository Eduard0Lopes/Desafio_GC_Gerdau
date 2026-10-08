package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "codigos_servico")
public class CodigoServico {

    @Id
    private String id;

    @Field("codigoOriginal")
    private String codigoOriginal;

    @Field("descricao")
    private String descricao;

    @Field("descricaoNormalizada")
    private String descricaoNormalizada;

    @Field("quantidadeCaracteres")
    private int quantidadeCaracteres;

    @Field("origem")
    private String origem;

}