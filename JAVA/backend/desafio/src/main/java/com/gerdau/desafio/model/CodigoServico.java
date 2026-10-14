package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = CodigoServico.COLECAO)
public class CodigoServico {

    public static final String COLECAO = "codigos_servico";
    public static final int TAMANHO_ID = 6;

    @Id
    private String id;
    private String descricao;
    private String descricaoNormalizada;
    private int quantidadeCaracteres;
    private Origem origem;
    private String justificativa;
    private List<String> legacyIds = new ArrayList<>();
}
