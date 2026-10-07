package br.com.gerdau.servicos.api.dto;

import br.com.gerdau.servicos.api.domain.DecisaoSimilaridade;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilaridadeResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CodigoCompletoDtos {

    private CodigoCompletoDtos() {}

    public record ComposicaoRequest(@NotBlank @Size(max = 32) String gsId,
                                    @NotBlank @Size(max = 32) String cfId,
                                    @NotBlank @Size(max = 32) String csId,
                                    @NotBlank @Size(max = 32) String umId) {}

    public record PreviewResponse(String idCompleto, String gsId, String cfId, String csId, String umId,
                                  String descricaoFinal, int quantidadeCaracteres, int limiteCaracteres,
                                  boolean dentroDoLimite, boolean existeCodigoCompleto,
                                  DecisaoSimilaridade decisaoRecomendada, SimilaridadeResponse similaridade) {}

    public record CodigoCompletoResponse(String idCompleto, String gsId, String cfId, String csId, String umId,
                                         String descricaoSnapshot, String usuarioCriacaoId, Instant createdAt) {}
}
