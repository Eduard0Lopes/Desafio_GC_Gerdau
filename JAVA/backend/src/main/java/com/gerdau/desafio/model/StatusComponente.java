package com.gerdau.desafio.model;

public final class StatusComponente {

    public static final String ATIVO = "ATIVO";
    public static final String PENDENTE_VALIDACAO = "PENDENTE_VALIDACAO";
    public static final String REJEITADO = "REJEITADO";

    public static final String DESATIVADO = "DESATIVADO";

    private StatusComponente() {
    }

    public static String normalizar(String status) {
        return (status == null || status.isBlank()) ? ATIVO : status.trim().toUpperCase();
    }

    public static boolean ativo(String status) {
        return ATIVO.equals(normalizar(status));
    }

    public static boolean desativado(String status) {
        return DESATIVADO.equals(normalizar(status));
    }
}
