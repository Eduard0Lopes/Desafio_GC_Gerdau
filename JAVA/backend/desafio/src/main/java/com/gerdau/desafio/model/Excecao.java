package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Registro de exceção/auditoria — documento técnico, seção 12.3. */
@Data
@Document(collection = "excecoes")
public class Excecao {

    @Id
    private String id;

    @Field("solicitante")
    private String solicitante;

    @Field("dataHora")
    private Instant dataHora;

    @Field("payloadOriginal")
    private Map<String, Object> payloadOriginal;

    @Field("similaresEncontrados")
    private List<RegistroSimilar> similaresEncontrados;

    @Field("justificativa")
    private String justificativa;

    @Field("decisaoTomada")
    private String decisaoTomada;

    @Field("observacao")
    private String observacao;

    @Field("codigoCompletoId")
    private String codigoCompletoId;
}
