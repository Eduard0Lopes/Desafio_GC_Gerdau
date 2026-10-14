package com.gerdau.desafio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Código SAP composto: GS (3) + CF (3) + CS (6) + UM (2) = 14 dígitos.
 */
@Data
@Document(collection = CodigoCompleto.COLECAO)
public class CodigoCompleto {

    public static final String COLECAO = "codigos_completos";

    @Id
    private String id;
    private String idCompleto;
    private String gsId;
    private String cfId;
    private String csId;
    private String umId;
    private String descricaoSnapshot;
    private String usuarioCriacaoId;
    private Instant createdAt;
    private Origem origem;
    private List<String> legacyIds = new ArrayList<>();

    public static String comporId(String gsId, String cfId, String csId, String umId) {
        return gsId + cfId + csId + umId;
    }
}
