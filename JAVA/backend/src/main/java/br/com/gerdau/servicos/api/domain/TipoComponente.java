package br.com.gerdau.servicos.api.domain;

/**
 * Os quatro componentes de um código completo (G.S., C.F., C.S., U.M.).
 * Se as coleções/campos do seu banco tiverem nomes diferentes, ajuste AQUI (ponto único).
 */
public enum TipoComponente {
    GS("grupos_servico", "Grupo de Serviço", "nome", "nomeNormalizado"),
    CF("codigos_fornecedor", "Código de Fornecedor", "nome", "nomeNormalizado"),
    CS("codigos_servico", "Código de Serviço", "descricao", "descricaoNormalizada"),
    UM("unidades_medida", "Unidade de Medida", "nome", "nomeNormalizado");

    private final String collection;
    private final String label;
    private final String textField;
    private final String normalizedField;

    TipoComponente(String collection, String label, String textField, String normalizedField) {
        this.collection = collection;
        this.label = label;
        this.textField = textField;
        this.normalizedField = normalizedField;
    }

    public String collection() { return collection; }
    public String label() { return label; }
    /** Campo com o texto exibido (nome ou descrição). */
    public String textField() { return textField; }
    /** Campo com o texto normalizado usado em busca e comparação. */
    public String normalizedField() { return normalizedField; }
}
