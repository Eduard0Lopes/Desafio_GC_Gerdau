package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "codigos_completos")
public class CodigoCompleto {

    public static final String STATUS_ATIVO = "ATIVO";
    public static final String STATUS_PENDENTE = "PENDENTE_VALIDACAO";
    public static final String STATUS_REJEITADO = "REJEITADO";
    public static final String STATUS_CANCELADO = "CANCELADO";

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("idCompleto")
    private String idCompleto;

    @Indexed
    @Field("gsId")
    private String gsId;

    @Indexed
    @Field("cfId")
    private String cfId;

    @Indexed
    @Field("csId")
    private String csId;

    @Indexed
    @Field("umId")
    private String umId;

    @Field("descricaoSnapshot")
    private String descricaoSnapshot;

    @Field("usuarioCriacaoId")
    private String usuarioCriacaoId;

    @Field("createdAt")
    private String createdAt;

    @Indexed
    @Field("origem")
    private String origem;

    @Field("legacyIds")
    private List<String> legacyIds;

    @Indexed
    @Field("status")
    private String status;

    @Field("decisaoSimilaridade")
    private String decisaoSimilaridade;

    @Field("melhorScore")
    private Double melhorScore;

    @Field("justificativa")
    private String justificativa;

    @Field("similaresRegistrados")
    private List<RegistroSimilar> similaresRegistrados;

    @Field("aprovadoPor")
    private String aprovadoPor;

    @Field("aprovadoEm")
    private Instant aprovadoEm;

    @Field("observacaoAprovacao")
    private String observacaoAprovacao;

    @Indexed(unique = true, sparse = true)
    @Field("idempotencyKey")
    private String idempotencyKey;

    @Field("idempotencyHash")
    private String idempotencyHash;
}
