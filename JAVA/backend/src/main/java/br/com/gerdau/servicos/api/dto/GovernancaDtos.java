package br.com.gerdau.servicos.api.dto;

import br.com.gerdau.servicos.api.domain.SolicitacaoGovernanca;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class GovernancaDtos {

    private GovernancaDtos() {}

    public enum Decisao { APROVAR, REJEITAR }

    public record SolicitarGovernancaRequest(@NotNull TipoComponente tipo,
                                             @Size(max = 200) String texto,
                                             @Size(max = 10) String sigla,
                                             @NotBlank @Size(min = 10, max = 500) String justificativa) {}

    public record DecisaoRequest(@NotNull Decisao decisao,
                                 @Size(max = 500) String observacao,
                                 @Size(max = 32) String itemSugeridoId) {}

    public record SolicitacaoResponse(String id, TipoComponente tipo, String texto, String sigla,
                                      String justificativa, SolicitacaoGovernanca.Status status,
                                      String solicitanteId, Instant createdAt, double scoreMaximo,
                                      List<Map<String, Object>> similares, String decisorId,
                                      Instant decididoEm, String observacaoDecisao,
                                      String itemCriadoId, String itemSugeridoId) {

        public static SolicitacaoResponse from(SolicitacaoGovernanca s) {
            return new SolicitacaoResponse(s.getId(), s.getTipo(), s.getTexto(), s.getSigla(),
                    s.getJustificativa(), s.getStatus(), s.getSolicitanteId(), s.getCreatedAt(),
                    s.getScoreMaximo(), s.getSimilares(), s.getDecisorId(), s.getDecididoEm(),
                    s.getObservacaoDecisao(), s.getItemCriadoId(), s.getItemSugeridoId());
        }
    }
}
