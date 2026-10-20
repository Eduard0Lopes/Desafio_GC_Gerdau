package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "eventos_auditoria")
public class EventoAuditoria {

    public static final String ACAO_CRIACAO_COMPONENTE = "CRIACAO_COMPONENTE";
    public static final String ACAO_EXCLUSAO_COMPONENTE = "EXCLUSAO_COMPONENTE";
    public static final String ACAO_DESATIVACAO_COMPONENTE = "DESATIVACAO_COMPONENTE";
    public static final String ACAO_REATIVACAO_COMPONENTE = "REATIVACAO_COMPONENTE";
    public static final String ACAO_CRIACAO_CODIGO_COMPLETO = "CRIACAO_CODIGO_COMPLETO";
    public static final String ACAO_BLOQUEIO = "BLOQUEIO";
    public static final String ACAO_EXCECAO = "EXCECAO";
    public static final String ACAO_APROVACAO = "APROVACAO";
    public static final String ACAO_REJEICAO = "REJEICAO";
    public static final String ACAO_REUTILIZACAO = "REUTILIZACAO";
    public static final String ACAO_REVISAO_HISTORICA = "REVISAO_HISTORICA";
    public static final String ACAO_ALTERACAO_PEDIDO = "ALTERACAO_PEDIDO";
    public static final String ACAO_CANCELAMENTO_PEDIDO = "CANCELAMENTO_PEDIDO";
    public static final String ACAO_REENVIO_PEDIDO = "REENVIO_PEDIDO";
    public static final String ACAO_TRANSFERENCIA_VINCULOS = "TRANSFERENCIA_VINCULOS";

    @Id
    private String id;

    @Indexed
    @Field("actorId")
    private String actorId;

    @Indexed
    @Field("action")
    private String action;

    @Indexed
    @Field("entityType")
    private String entityType;

    @Field("entityId")
    private String entityId;

    @Indexed
    @Field("occurredAt")
    private Instant occurredAt;

    @Field("decision")
    private String decision;

    @Field("reason")
    private String reason;

    @Field("candidateIds")
    private List<String> candidateIds;

    @Field("similarityScores")
    private List<Double> similarityScores;

    @Indexed
    @Field("correlationId")
    private String correlationId;
}
