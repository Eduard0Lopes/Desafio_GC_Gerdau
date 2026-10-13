package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Document(collection = "codigos_completos")
public class CodigoCompleto {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("idCompleto")
    private String idCompleto;

    @Field("gsId")
    private String gsId;

    @Field("cfId")
    private String cfId;

    @Field("csId")
    private String csId;

    @Field("umId")
    private String umId;

    @Field("descricaoSnapshot")
    private String descricaoSnapshot;

    @Field("usuarioCriacaoId")
    private String usuarioCriacaoId;

    @Field("createdAt")
    private String createdAt;

    @Field("origem")
    private String origem;

    @Field("legacyIds")
    private List<String> legacyIds;

    @Field("cenarioTeste")
    private String cenarioTeste;
}