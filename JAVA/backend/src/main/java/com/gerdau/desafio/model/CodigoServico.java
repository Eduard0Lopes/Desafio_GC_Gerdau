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

    /**
     * Caso especial (badge "(!)" na interface): o item recebeu uma marca humana —
     * exceção aprovada à política de similaridade OU duplicidade tratada na varredura.
     * A marca acompanha o item em todas as telas; o motivo aparece no hover do badge.
     * Pode ser resolvida (limpa a marca) ou levada à exclusão pelo painel do catálogo.
     */
    @Field("casoIsolado")
    private Boolean casoIsolado = false;

    /** Motivo registrado ao marcar o caso — é o texto exibido no hover do (!). */
    @Field("motivoCasoIsolado")
    private String motivoCasoIsolado;

    @Field("criadoPor")
    private String criadoPor;

    @Field("createdAt")
    private Instant createdAt;

    @Field("updatedAt")
    private Instant updatedAt;
}
