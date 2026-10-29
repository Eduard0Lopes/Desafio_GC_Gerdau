package com.gerdau.desafio.util;

public final class ComposicaoUtil {

    private ComposicaoUtil() {
        throw new UnsupportedOperationException("Classe utilitária — não instancie.");
    }

    public static String montarIdCompleto(String grupoServico, String fornecedor, String servico, String unidade) {
        return grupoServico + "_" + fornecedor + "_" + servico + "_" + unidade;
    }

    public static String montarSnapshot(String grupoServico, String fornecedor, String servico, String unidade) {
        return grupoServico + " | " + fornecedor + " | " + servico + " | " + unidade;
    }
}
