package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
    @Document(collection = "codigos_fornecedor")
    public class CodigosFornecedor {

        @Id
        private String id;

        @Field("codigoOriginal")
        private String codigoOriginal;

        @Field("nomeNormalizado")
        private String nomeNormalizado;

        @Field("origem")
        private String origem;
    }
