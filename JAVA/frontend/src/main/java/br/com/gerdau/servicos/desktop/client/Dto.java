package br.com.gerdau.servicos.desktop.client;

import java.util.List;

/** Contratos da API (espelham os DTOs do backend). Campos desconhecidos são ignorados. */
public final class Dto {

    private Dto() {}

    public enum Tipo {
        GS("Grupo de Serviço", "grupos-servico"),
        CF("Código de Fornecedor", "codigos-fornecedor"),
        CS("Código de Serviço", "codigos-servico"),
        UM("Unidade de Medida", "unidades-medida");

        private final String label;
        private final String path;

        Tipo(String label, String path) {
            this.label = label;
            this.path = path;
        }

        public String label() { return label; }
        public String path() { return path; }

        /** Usado pelo ComboBox. */
        @Override
        public String toString() { return label; }
    }

    public record Me(String username, List<String> roles) {}

    public record Componente(String id, Tipo tipo, String texto, String sigla, String rotulo,
                             Double score, boolean aproximado) {}

    public record Composicao(String gsId, String cfId, String csId, String umId) {}

    public record SimilarItem(String id, String texto, String sigla, double score, String motivo) {
        public String rotulo(Tipo tipo) {
            return tipo == Tipo.UM && sigla != null && !sigla.isBlank() ? sigla + " - " + texto : id + " - " + texto;
        }
    }

    public record Similaridade(String decisao, double scoreMaximo, String enforcement, double limiarAlerta,
                               double limiarBloqueio, boolean aproximado, List<SimilarItem> similares) {
        public boolean observando() { return "OBSERVE".equalsIgnoreCase(enforcement); }
    }

    public record Preview(String idCompleto, String gsId, String cfId, String csId, String umId,
                          String descricaoFinal, int quantidadeCaracteres, int limiteCaracteres,
                          boolean dentroDoLimite, boolean existeCodigoCompleto, String decisaoRecomendada,
                          Similaridade similaridade) {}

    public record CodigoCompleto(String idCompleto, String descricaoSnapshot) {}

    public record ItemCriado(Componente item, String decisaoSimilaridade, boolean excecaoRegistrada) {}

    public record Solicitacao(String id, String status) {}
}
