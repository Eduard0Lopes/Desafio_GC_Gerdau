package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = CodigoFornecedor.COLECAO)
public class CodigoFornecedor {

    public static final String COLECAO = "codigos_fornecedor";
    public static final int TAMANHO_ID = 3;

    @Id
    private String id;
    private Long codigoFornecedorSAP;
    private String nome;
    private String nomeNormalizado;
    private Origem origem;
}
