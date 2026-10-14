package com.gerdau.desafio.service.similaridade;

import com.gerdau.desafio.dto.response.SimilaridadeResponse;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import com.gerdau.desafio.util.TextoUtil;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class SimilaridadeService {

    private final CodigoServicoRepository repository;

    public SimilaridadeService(CodigoServicoRepository repository) {
        this.repository = repository;
    }

    public List<SimilaridadeResponse> buscarSimilares(String descricao, int limite, String ignorarId) {
        String normalizada = TextoUtil.normalizar(descricao);
        if (normalizada.isEmpty()) {
            return List.of();
        }
        return repository.findAllDescricoes().stream()
                .filter(cs -> !Objects.equals(cs.getId(), ignorarId))
                .map(cs -> avaliar(normalizada, cs))
                .sorted(Comparator.comparingDouble(SimilaridadeResponse::score).reversed()
                        .thenComparing(SimilaridadeResponse::id))
                .limit(limite)
                .toList();
    }

    public Optional<SimilaridadeResponse> maisSimilar(String descricao, String ignorarId) {
        return buscarSimilares(descricao, 1, ignorarId).stream().findFirst();
    }

    private SimilaridadeResponse avaliar(String normalizada, CodigoServico candidato) {
        double score = arredondar(FuzzyEngine.similaridade(normalizada, candidato.getDescricaoNormalizada()));
        StatusSimilaridade status = RankingPolicy.classificar(score);
        return new SimilaridadeResponse(candidato.getId(), candidato.getDescricao(), score, status,
                status.getAcaoRecomendada());
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10_000d) / 10_000d;
    }
}
