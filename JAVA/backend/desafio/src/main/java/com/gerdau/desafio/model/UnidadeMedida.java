package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = UnidadeMedida.COLECAO)
public class UnidadeMedida {

    public static final String COLECAO = "unidades_medida";
    public static final int TAMANHO_ID = 2;

    @Id
    private String id;
    private String sigla;
    private String nome;
    private String nomeNormalizado;
    private Origem origem;
}
