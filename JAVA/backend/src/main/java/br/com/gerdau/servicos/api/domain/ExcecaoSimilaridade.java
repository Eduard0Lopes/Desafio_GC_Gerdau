package br.com.gerdau.servicos.api.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Registro de exceção (doc. seção 12.3): solicitante, data/hora, payload original, similares encontrados,
 * justificativa, decisão e observação. Serve para rastreabilidade e revisão posterior.
 */
@Document("excecoes_similaridade")
public class ExcecaoSimilaridade {

    @Id
    private String id;
    private TipoComponente tipo;
    private Map<String, Object> payload;
    private List<Map<String, Object>> similares;
    private double scoreMaximo;
    private DecisaoSimilaridade decisaoCalculada;
    /** PROSSEGUIU_COM_JUSTIFICATIVA | OBSERVACAO | APROVADO_GOVERNANCA */
    private String decisaoTomada;
    private String justificativa;
    private String observacao;
    private String solicitanteId;
    private String itemCriadoId;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public TipoComponente getTipo() { return tipo; }
    public void setTipo(TipoComponente tipo) { this.tipo = tipo; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public List<Map<String, Object>> getSimilares() { return similares; }
    public void setSimilares(List<Map<String, Object>> similares) { this.similares = similares; }
    public double getScoreMaximo() { return scoreMaximo; }
    public void setScoreMaximo(double scoreMaximo) { this.scoreMaximo = scoreMaximo; }
    public DecisaoSimilaridade getDecisaoCalculada() { return decisaoCalculada; }
    public void setDecisaoCalculada(DecisaoSimilaridade decisaoCalculada) { this.decisaoCalculada = decisaoCalculada; }
    public String getDecisaoTomada() { return decisaoTomada; }
    public void setDecisaoTomada(String decisaoTomada) { this.decisaoTomada = decisaoTomada; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(String solicitanteId) { this.solicitanteId = solicitanteId; }
    public String getItemCriadoId() { return itemCriadoId; }
    public void setItemCriadoId(String itemCriadoId) { this.itemCriadoId = itemCriadoId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
