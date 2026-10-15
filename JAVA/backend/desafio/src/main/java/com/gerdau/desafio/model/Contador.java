package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** Sequência atômica para IDs técnicos de novos componentes criados pela aplicação (seção 8.2). */
@Data
@Document(collection = "contadores")
public class Contador {

    @Id
    private String id;

    @Field("valor")
    private long valor;
}
