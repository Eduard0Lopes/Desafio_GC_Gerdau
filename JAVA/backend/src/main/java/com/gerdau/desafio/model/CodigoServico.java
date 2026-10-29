package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "codigos_servico")
public class CodigoServico {

    @Id
    private String id;

    @Field("descricao")
    private String descricao;

    @Indexed
    @Field("descricaoNormalizada")
    private String descricaoNormalizada;

    @Field("quantidadeCaracteres")
    private int quantidadeCaracteres;

    @Indexed
    @Field("origem")
    private String origem;

    @Indexed
    @Field("status")
    private String status = StatusComponente.ATIVO;

    @Field("legacyIds")
    private List<String> legacyIds;

    @Indexed
    @Field("natureza")
    private String natureza;

    @Field("active")
    private Boolean active = true;

    @Field("casoIsolado")
    private Boolean casoIsolado = false;

    @Field("motivoCasoIsolado")
    private String motivoCasoIsolado;

    @Field("criadoPor")
    private String criadoPor;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;
}
