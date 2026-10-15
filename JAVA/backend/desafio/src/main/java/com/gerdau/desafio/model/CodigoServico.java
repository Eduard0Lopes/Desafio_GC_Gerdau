package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Componente C.S. — documento técnico, seção 7.3 (limite SAP de 40 caracteres).
 * Identidade oficial = newId da matriz do Time de IA (ex.: "CS-130835").
 */
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

    /** Códigos legados consolidados neste item canônico (seções 7.3 e 9.3). */
    @Field("legacyIds")
    private List<String> legacyIds;

    /** Natureza do serviço (mão de obra, material, equipamento, locação, outros). */
    @Indexed
    @Field("natureza")
    private String natureza;

    @Field("active")
    private Boolean active = true;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;
}
