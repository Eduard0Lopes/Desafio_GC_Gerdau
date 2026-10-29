package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Document(collection = "matriz_depara")
public class MatrizDePara {

    public static final String STATUS_PENDENTE = "PENDENTE";

    public static final String STATUS_REVISADO = "REVISADO";

    public static final String STATUS_SEM_PENDENCIA = "SEM_PENDENCIA";

    @Id
    private String id;

    @Indexed
    @Field("newId")
    private String newId;

    @Field("descricaoCanonica")
    private String descricaoCanonica;

    @Indexed
    @Field("grupoServico")
    private String grupoServico;

    @Indexed
    @Field("unidadeMedida")
    private String unidadeMedida;

    @Indexed
    @Field("decision")
    private String decision;

    @Indexed
    @Field("natureza")
    private String natureza;

    @Field("score")
    private Double score;

    @Field("reason")
    private String reason;

    @Indexed
    @Field("legacyData")
    private LegacyData legacyData;

    @Indexed
    @Field("statusRevisao")
    private String statusRevisao;

    @Field("reviewedBy")
    private String reviewedBy;

    @Field("reviewedAt")
    private Instant reviewedAt;

    @Field("observacaoRevisao")
    private String observacaoRevisao;

    @Data
    public static class LegacyData {

        @Indexed
        @Field("legacyId")
        private String legacyId;

        @Field("legacyDescription")
        private String legacyDescription;

        @Field("legacySource")
        private String legacySource;

        @Indexed
        @Field("codigoFornecedor")
        private Long codigoFornecedor;

        @Field("alertaSap")
        private Boolean alertaSap;

        @Field("quantidadeCaracteres")
        private Integer quantidadeCaracteres;
    }
}
