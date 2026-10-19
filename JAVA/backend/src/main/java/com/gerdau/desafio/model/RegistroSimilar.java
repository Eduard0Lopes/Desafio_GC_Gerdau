package com.gerdau.desafio.model;

import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Snapshot de um item similar encontrado no momento do cadastro.
 * Classe simples (não record) de propósito: documentos antigos, gravados sem os
 * campos novos, continuam sendo lidos — com os padrões declarados abaixo.
 */
public class RegistroSimilar {

    @Field("posicao")
    private int posicao;

    @Field("id")
    private String id;

    @Field("codigoOriginal")
    private String codigoOriginal;

    @Field("descricao")
    private String descricao;

    @Field("score")
    private double score;

    @Field("motivo")
    private String motivo;

    @Field("casoIsolado")
    private boolean casoIsolado = false;

    @Field("motivoCasoIsolado")
    private String motivoCasoIsolado;

    public RegistroSimilar() {
    }

    public RegistroSimilar(int posicao, String id, String codigoOriginal, String descricao,
                           double score, String motivo, boolean casoIsolado, String motivoCasoIsolado) {
        this.posicao = posicao;
        this.id = id;
        this.codigoOriginal = codigoOriginal;
        this.descricao = descricao;
        this.score = score;
        this.motivo = motivo;
        this.casoIsolado = casoIsolado;
        this.motivoCasoIsolado = motivoCasoIsolado;
    }

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigoOriginal() {
        return codigoOriginal;
    }

    public void setCodigoOriginal(String codigoOriginal) {
        this.codigoOriginal = codigoOriginal;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isCasoIsolado() {
        return casoIsolado;
    }

    public void setCasoIsolado(boolean casoIsolado) {
        this.casoIsolado = casoIsolado;
    }

    public String getMotivoCasoIsolado() {
        return motivoCasoIsolado;
    }

    public void setMotivoCasoIsolado(String motivoCasoIsolado) {
        this.motivoCasoIsolado = motivoCasoIsolado;
    }
}
