package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "codigos_completos")
public class CodigoCompleto {

    @Id
    private String id;

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

    @Field("grupoServicoOriginal")
    private String grupoServicoOriginal;

    @Field("codigoFornecedorOriginal")
    private String codigoFornecedorOriginal;

    @Field("codigoServicoOriginal")
    private String codigoServicoOriginal;

    @Field("unidadeOriginal")
    private String unidadeOriginal;

    @Field("descricaoSnapshot")
    private String descricaoSnapshot;

    @Field("usuarioCriacaoId")
    private String usuarioCriacaoId;

    @Field("origem")
    private String origem;

    @Field("cenarioTeste")
    private String cenarioTeste;
}