package com.gerdau.desafio.search;

import com.gerdau.desafio.dto.response.CandidatoSimilarResponse;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.similarity.CatalogoItem;
import com.gerdau.desafio.similarity.RankingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor local (padrão): port fiel do fuzzy engine do Time de IA
 * ({@code IA/src/fuzzy_engine.py} — 40% Jaccard de tokens + 60% Ratcliff/Obershelp),
 * executado sobre os catálogos oficiais em memória.
 */
@Component
@ConditionalOnProperty(name = "similaridade.motor", havingValue = "local", matchIfMissing = true)
public class LocalSearchClient implements SearchClient {

    private static final Logger log = LoggerFactory.getLogger(LocalSearchClient.class);

    private final CodigoServicoRepository codigoServicoRepository;
    private final GrupoServicoRepository grupoServicoRepository;
    private final CodigoFornecedorRepository codigoFornecedorRepository;
    private final UnidadeMedidaRepository unidadeMedidaRepository;

    private final Map<String, List<CatalogoItem>> caches = new ConcurrentHashMap<>();

    public LocalSearchClient(CodigoServicoRepository codigoServicoRepository,
                             GrupoServicoRepository grupoServicoRepository,
                             CodigoFornecedorRepository codigoFornecedorRepository,
                             UnidadeMedidaRepository unidadeMedidaRepository) {
        this.codigoServicoRepository = codigoServicoRepository;
        this.grupoServicoRepository = grupoServicoRepository;
        this.codigoFornecedorRepository = codigoFornecedorRepository;
        this.unidadeMedidaRepository = unidadeMedidaRepository;
    }

    @Override
    public String mecanismo() {
        return "local";
    }

    @Override
    public List<CandidatoSimilarResponse> buscarSimilares(String consulta, int limite, double scoreMinimo, String tipo) {
        return RankingEngine.ranquear(consulta, carregarCatalogo(tipo), limite, scoreMinimo);
    }

    @Override
    public void invalidarCache() {
        caches.clear();
    }

    /** Pré-carrega os catálogos na subida — a primeira busca já responde instantaneamente. */
    @org.springframework.context.event.EventListener(
            org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void aquecerCache() {
        try {
            long inicio = System.currentTimeMillis();
            for (String tipo : List.of(TIPO_CS, TIPO_GS, TIPO_CF, TIPO_UM)) {
                carregarCatalogo(tipo);
            }
            log.info("[SEARCH/local] Catálogos pré-carregados em {} ms", System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            log.warn("[SEARCH/local] Pré-carregamento adiado ({}).", e.getMessage());
        }
    }

    private List<CatalogoItem> carregarCatalogo(String tipo) {
        String chave = (tipo == null || tipo.isBlank()) ? TIPO_CS : tipo.trim().toUpperCase();
        return caches.computeIfAbsent(chave, this::montar);
    }

    private List<CatalogoItem> montar(String tipo) {
        List<CatalogoItem> itens = switch (tipo) {
            case TIPO_GS -> grupoServicoRepository.findAll().stream()
                    .map((GrupoServico g) -> RankingEngine.prepararItem(g.getId(), null, null,
                            g.getNome(), g.getNomeNormalizado()))
                    .toList();
            case TIPO_CF -> codigoFornecedorRepository.findAll().stream()
                    .map((CodigoFornecedor f) -> RankingEngine.prepararItem(f.getId(), String.valueOf(f.getCodigoFornecedorSAP()), null,
                            f.getNome(), f.getNomeNormalizado()))
                    .toList();
            case TIPO_UM -> unidadeMedidaRepository.findAll().stream()
                    .map((UnidadeMedida u) -> RankingEngine.prepararItem(u.getId(), null, null,
                            u.getSigla() + " - " + u.getNome(), null)) // sigla distingue as unidades
                    .toList();
            default -> codigoServicoRepository.findAll().stream()
                    .map(c -> RankingEngine.prepararItem(c.getId(), null, c.getLegacyIds(),
                            c.getDescricao(), c.getDescricaoNormalizada()))
                    .toList();
        };
        log.info("[SEARCH/local] Catálogo {} carregado: {} itens", tipo, itens.size());
        return itens;
    }
}
