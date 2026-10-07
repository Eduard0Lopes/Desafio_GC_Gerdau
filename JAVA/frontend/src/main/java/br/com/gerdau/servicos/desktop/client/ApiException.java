package br.com.gerdau.servicos.desktop.client;

/** Erro devolvido pela API (ou falha de rede, quando status == 0). */
public final class ApiException extends RuntimeException {

    private final int status;
    private final String codigo;
    private final Dto.Similaridade similaridade;

    public ApiException(int status, String codigo, String message, Dto.Similaridade similaridade) {
        super(message);
        this.status = status;
        this.codigo = codigo;
        this.similaridade = similaridade;
    }

    public int status() { return status; }
    public String codigo() { return codigo; }
    /** Presente nos erros 409 SIMILARIDADE_ALTA / SIMILARIDADE_INTERMEDIARIA. */
    public Dto.Similaridade similaridade() { return similaridade; }
    public boolean isNetworkError() { return status == 0; }
}
