package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = GrupoServico.COLECAO)
public class GrupoServico {

    public static final String COLECAO = "grupos_servico";
    public static final int TAMANHO_ID = 3;

    @Id
    private String id;
    private String nome;
    private String nomeNormalizado;
    private Origem origem;
}
