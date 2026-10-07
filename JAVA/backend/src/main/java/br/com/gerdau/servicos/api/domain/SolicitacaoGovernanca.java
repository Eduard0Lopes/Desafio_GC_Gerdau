package br.com.gerdau.servicos.api.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Pedido de criação de item enviado à Equipe de Governança de Dados (validação humana obrigatória). */
@Document("solicitacoes_governanca")
public class SolicitacaoGovernanca {

    public enum Status { PENDENTE, APROVADA, REJEITADA }

    @Id
    private String id;
    @Version
    private Long version;
    private TipoComponente tipo;
    private String texto;
    private String sigla;
    private String justificativa;
    private Status status = Status.PENDENTE;
    private String solicitanteId;
    private Instant createdAt;
    private double scoreMaximo;
    private List<Map<String, Object>> similares;
    private String decisorId;
    private Instant decididoEm;
    private String observacaoDecisao;
    private String itemCriadoId;
    private String itemSugeridoId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public TipoComponente getTipo() { return tipo; }
    public void setTipo(TipoComponente tipo) { this.tipo = tipo; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(String solicitanteId) { this.solicitanteId = solicitanteId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public double getScoreMaximo() { return scoreMaximo; }
    public void setScoreMaximo(double scoreMaximo) { this.scoreMaximo = scoreMaximo; }
    public List<Map<String, Object>> getSimilares() { return similares; }
    public void setSimilares(List<Map<String, Object>> similares) { this.similares = similares; }
    public String getDecisorId() { return decisorId; }
    public void setDecisorId(String decisorId) { this.decisorId = decisorId; }
    public Instant getDecididoEm() { return decididoEm; }
    public void setDecididoEm(Instant decididoEm) { this.decididoEm = decididoEm; }
    public String getObservacaoDecisao() { return observacaoDecisao; }
    public void setObservacaoDecisao(String observacaoDecisao) { this.observacaoDecisao = observacaoDecisao; }
    public String getItemCriadoId() { return itemCriadoId; }
    public void setItemCriadoId(String itemCriadoId) { this.itemCriadoId = itemCriadoId; }
    public String getItemSugeridoId() { return itemSugeridoId; }
    public void setItemSugeridoId(String itemSugeridoId) { this.itemSugeridoId = itemSugeridoId; }
}
