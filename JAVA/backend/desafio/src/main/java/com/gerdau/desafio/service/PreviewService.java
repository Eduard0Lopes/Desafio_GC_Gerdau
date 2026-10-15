package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.dto.response.*;
import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.search.SearchClient;
import com.gerdau.desafio.seed.DataSeeder;
import com.gerdau.desafio.similarity.RankingPolicy;
import com.gerdau.desafio.util.TextoUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Prévia do código completo antes de salvar (seção 10.2): resolve os quatro
 * componentes (existentes ou novos), monta o idCompleto com os IDs oficiais,
 * analisa similaridade em TODOS os campos e recomenda a decisão
 * (PERMITIR | ALERTAR | REVISAR).
 */
@Service
public class PreviewService {

    public static final String ACAO_CRIAR = "CRIAR";
    public static final String ACAO_ENVIAR_VALIDACAO = "ENVIAR_VALIDACAO";

    /** Marcador usado na prévia para componentes que seriam criados (não aloca IDs). */
    public static final String MARCADOR_NOVO = "(novo)";

    private final GrupoServicoRepository grupoServicoRepository;
    private final CodigoFornecedorRepository codigoFornecedorRepository;
    private final CodigoServicoRepository codigoServicoRepository;
    private final UnidadeMedidaRepository unidadeMedidaRepository;
    private final CodigoCompletoRepository codigoCompletoRepository;
    private final SimilaridadeService similaridadeService;
    private final RankingPolicy policy;

    public record ComponentesResolvidos(
            GrupoServico grupoServico, boolean grupoNovo,
            CodigoFornecedor codigoFornecedor, boolean fornecedorNovo,
            CodigoServico codigoServico, boolean codigoNovo,
            UnidadeMedida unidadeMedida, boolean unidadeNova) {
    }

    public PreviewService(GrupoServicoRepository grupoServicoRepository,
                          CodigoFornecedorRepository codigoFornecedorRepository,
                          CodigoServicoRepository codigoServicoRepository,
                          UnidadeMedidaRepository unidadeMedidaRepository,
                          CodigoCompletoRepository codigoCompletoRepository,
                          SimilaridadeService similaridadeService,
                          RankingPolicy policy) {
        this.grupoServicoRepository = grupoServicoRepository;
        this.codigoFornecedorRepository = codigoFornecedorRepository;
        this.codigoServicoRepository = codigoServicoRepository;
        this.unidadeMedidaRepository = unidadeMedidaRepository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.similaridadeService = similaridadeService;
        this.policy = policy;
    }

    /**
     * Resolve os componentes sem persistir.
     *
     * <p>Um dado "novo" que já existe no catálogo (mesmo nome/sigla) REUTILIZA o item
     * existente em vez de tentar criar outro — é a regra "Reutilize o item existente"
     * aplicada automaticamente, evitando erro de duplicidade e cadastros parciais.</p>
     *
     * @param alocarIds {@code false} na prévia (componentes novos recebem {@link #MARCADOR_NOVO})
     */
    public ComponentesResolvidos resolver(PreviewRequest req, boolean alocarIds) {
        GrupoServico gs;
        boolean grupoNovo = false;
        if (naoVazio(req.gsId())) {
            gs = grupoServicoRepository.findById(req.gsId())
                    .orElseThrow(() -> new BusinessException("Grupo de Serviço não encontrado: " + req.gsId()));
        } else if (naoVazio(req.gsNovoNome())) {
            String nome = req.gsNovoNome().trim();
            GrupoServico existente = buscarGrupoExistente(nome);
            if (existente != null) {
                gs = existente;
            } else {
                gs = new GrupoServico();
                gs.setId(alocarIds ? DataSeeder.slug(nome) : MARCADOR_NOVO);
                gs.setNome(nome);
                gs.setNomeNormalizado(TextoUtil.normalizar(nome));
                gs.setOrigem("SISTEMA");
                grupoNovo = true;
            }
        } else {
            throw new BusinessException("Informe o Grupo de Serviço (id) ou o nome de um novo grupo.");
        }

        CodigoFornecedor cf;
        boolean fornecedorNovo = false;
        if (naoVazio(req.cfId())) {
            cf = codigoFornecedorRepository.findById(req.cfId())
                    .orElseThrow(() -> new BusinessException("Código de Fornecedor não encontrado: " + req.cfId()));
        } else if (naoVazio(req.cfNovoNome())) {
            String nome = req.cfNovoNome().trim();
            String sap = naoVazio(req.cfNovoCodigoSap()) ? req.cfNovoCodigoSap().trim() : null;
            CodigoFornecedor existente = buscarFornecedorExistente(nome, sap);
            if (existente != null) {
                cf = existente;
            } else {
                cf = new CodigoFornecedor();
                String id = sap != null ? sap : String.valueOf(System.currentTimeMillis() % 10_000_000);
                cf.setId(alocarIds ? id : MARCADOR_NOVO);
                if (alocarIds) {
                    cf.setCodigoFornecedorSAP(Long.parseLong(id));
                }
                cf.setNome(nome);
                cf.setNomeNormalizado(TextoUtil.normalizar(nome));
                cf.setOrigem("SISTEMA");
                fornecedorNovo = true;
            }
        } else {
            throw new BusinessException("Informe o Código de Fornecedor (id) ou o nome de um novo fornecedor.");
        }

        CodigoServico cs;
        boolean codigoNovo = false;
        if (naoVazio(req.csId())) {
            cs = codigoServicoRepository.findById(req.csId())
                    .orElseThrow(() -> new BusinessException("Código de Serviço não encontrado: " + req.csId()));
        } else if (naoVazio(req.csNovaDescricao())) {
            String descricao = req.csNovaDescricao().trim();
            if (descricao.length() > TextoUtil.LIMITE_SAP) {
                throw new BusinessException("A descrição do serviço não pode exceder "
                        + TextoUtil.LIMITE_SAP + " caracteres (regra SAP). Atual: " + descricao.length() + ".");
            }
            cs = new CodigoServico();
            cs.setId(MARCADOR_NOVO); // id oficial (CS-XXXXXX) é alocado apenas na criação
            cs.setDescricao(descricao);
            cs.setDescricaoNormalizada(TextoUtil.normalizar(descricao));
            cs.setQuantidadeCaracteres(descricao.length());
            cs.setOrigem("SISTEMA");
            codigoNovo = true;
        } else {
            throw new BusinessException("Informe o Código de Serviço (id) ou a descrição de um novo serviço.");
        }

        UnidadeMedida um;
        boolean unidadeNova = false;
        if (naoVazio(req.umId())) {
            um = unidadeMedidaRepository.findById(req.umId())
                    .orElseThrow(() -> new BusinessException("Unidade de Medida não encontrada: " + req.umId()));
        } else if (naoVazio(req.umNovaSigla())) {
            String sigla = req.umNovaSigla().trim().toUpperCase();
            UnidadeMedida existente = unidadeMedidaRepository.findById(sigla).orElse(null);
            if (existente != null) {
                um = existente;
            } else {
                um = new UnidadeMedida();
                um.setId(alocarIds ? sigla : MARCADOR_NOVO);
                um.setSigla(sigla);
                um.setNome(naoVazio(req.umNovoNome()) ? req.umNovoNome().trim() : sigla);
                um.setNomeNormalizado(TextoUtil.normalizar(um.getNome()));
                um.setOrigem("SISTEMA");
                unidadeNova = true;
            }
        } else {
            throw new BusinessException("Informe a Unidade de Medida (id) ou a sigla de uma nova unidade.");
        }

        return new ComponentesResolvidos(gs, grupoNovo, cf, fornecedorNovo, cs, codigoNovo, um, unidadeNova);
    }

    /** Grupo existente com o mesmo nome (identidade do G.S. = nome/slug) — reutiliza em vez de duplicar. */
    private GrupoServico buscarGrupoExistente(String nome) {
        GrupoServico porId = grupoServicoRepository.findById(DataSeeder.slug(nome)).orElse(null);
        if (porId != null) {
            return porId;
        }
        return grupoServicoRepository.findByNomeNormalizado(TextoUtil.normalizar(nome))
                .stream().findFirst().orElse(null);
    }

    /**
     * Fornecedor existente: mesmo código SAP (identidade) ou — quando o SAP não foi
     * informado — mesmo nome. Com SAP novo, o fornecedor é outro mesmo com nome igual.
     */
    private CodigoFornecedor buscarFornecedorExistente(String nome, String codigoSap) {
        if (codigoSap != null) {
            return codigoFornecedorRepository.findById(codigoSap).orElse(null);
        }
        return codigoFornecedorRepository.findByNomeNormalizado(TextoUtil.normalizar(nome))
                .stream().findFirst().orElse(null);
    }

    public PreviewResponse montarPreview(PreviewRequest req) {
        ComponentesResolvidos r = resolver(req, false);

        String idCompleto = montarIdCompleto(r);
        boolean todosExistentes = !r.grupoNovo() && !r.fornecedorNovo() && !r.codigoNovo() && !r.unidadeNova();
        boolean jaExiste = todosExistentes && codigoCompletoRepository.existsByIdCompleto(idCompleto);

        String descricao = r.codigoServico().getDescricao();
        boolean excedeSap = TextoUtil.excedeLimiteSap(descricao);

        // ---------- anti-duplicidade em TODOS os componentes novos ----------
        List<AlertaComponenteResponse> alertas = new ArrayList<>();
        if (r.grupoNovo()) {
            analisarComponente(SearchClient.TIPO_GS, r.grupoServico().getNome(), alertas);
        }
        if (r.fornecedorNovo()) {
            analisarComponente(SearchClient.TIPO_CF, r.codigoFornecedor().getNome(), alertas);
        }
        if (r.unidadeNova()) {
            analisarComponente(SearchClient.TIPO_UM,
                    r.unidadeMedida().getSigla() + " " + r.unidadeMedida().getNome(), alertas);
        }

        List<CandidatoSimilarResponse> similares;
        double melhorScore = 0.0;
        String status;
        String decisao;
        String acaoRecomendada;

        if (r.codigoNovo()) {
            SimilaridadeResponse sim = similaridadeService.consultar(descricao, 5, SearchClient.TIPO_CS);
            similares = sim.candidatos();
            melhorScore = similares.isEmpty() ? 0.0 : similares.get(0).score();
            status = sim.status();
            decisao = sim.decisao();
            acaoRecomendada = sim.acaoRecomendada();
        } else {
            similares = List.of();
            status = RankingPolicy.ORIGINAL;
            decisao = "PERMITIR";
            acaoRecomendada = "Reutilização de código existente — sem risco de duplicidade no Código de Serviço.";
        }

        for (AlertaComponenteResponse alerta : alertas) {
            if (severidade(alerta.decisao()) > severidade(decisao)) {
                decisao = alerta.decisao();
                status = "REVISAR".equals(alerta.decisao()) ? RankingPolicy.DUPLICIDADE : RankingPolicy.ALERTA;
                acaoRecomendada = nomeComponente(alerta.tipo()) + " semelhante a \"" + alerta.itemSimilarNome()
                        + "\" (" + Math.round(alerta.score() * 100) + "%). Reutilize o existente ou justifique.";
            }
        }

        if (jaExiste) {
            status = RankingPolicy.DUPLICIDADE;
            decisao = "REVISAR";
            acaoRecomendada = "Já existe um código completo com esta composição (GS+CF+CS+UM). Reutilize o existente.";
        }

        return new PreviewResponse(
                idCompleto,
                new ComponentePreviewResponse(r.grupoServico().getId(), r.grupoServico().getNome(), r.grupoNovo()),
                new ComponentePreviewResponse(r.codigoFornecedor().getId(), r.codigoFornecedor().getNome(),
                        r.fornecedorNovo()),
                new ComponentePreviewResponse(r.codigoServico().getId(), r.codigoServico().getDescricao(),
                        r.codigoNovo()),
                new ComponentePreviewResponse(r.unidadeMedida().getId(),
                        r.unidadeMedida().getSigla() + " - " + r.unidadeMedida().getNome(), r.unidadeNova()),
                montarSnapshot(r),
                descricao.length(),
                excedeSap,
                jaExiste,
                status,
                decisao,
                acaoRecomendada,
                melhorScore,
                similares,
                List.copyOf(alertas));
    }

    /** Composição do idCompleto com os IDs oficiais dos quatro componentes (seção 8.3). */
    public static String montarIdCompleto(ComponentesResolvidos r) {
        return r.grupoServico().getId() + "_" + r.codigoFornecedor().getId() + "_"
                + r.codigoServico().getId() + "_" + r.unidadeMedida().getId();
    }

    public String montarSnapshot(ComponentesResolvidos r) {
        return r.grupoServico().getNome() + " | " + r.codigoFornecedor().getNome() + " | "
                + r.codigoServico().getDescricao() + " | " + r.unidadeMedida().getSigla();
    }

    private void analisarComponente(String tipo, String texto, List<AlertaComponenteResponse> alertas) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        SimilaridadeResponse sim = similaridadeService.consultar(texto, 3, tipo);
        if (sim.candidatos().isEmpty()) {
            return;
        }
        CandidatoSimilarResponse melhor = sim.candidatos().get(0);
        alertas.add(new AlertaComponenteResponse(tipo, texto.trim(), melhor.id(), melhor.descricao(),
                melhor.score(), sim.decisao(), melhor.motivo()));
    }

    private static int severidade(String decisao) {
        return switch (decisao) {
            case "REVISAR" -> 2;
            case "ALERTAR" -> 1;
            default -> 0;
        };
    }

    /** Nome completo do componente (sem abreviações — linguagem clara para o usuário). */
    public static String nomeComponente(String tipo) {
        return switch (tipo) {
            case SearchClient.TIPO_GS -> "Grupo de Serviço";
            case SearchClient.TIPO_CF -> "Código de Fornecedor";
            case SearchClient.TIPO_UM -> "Unidade de Medida";
            default -> "Código de Serviço";
        };
    }

    private static boolean naoVazio(String s) {
        return s != null && !s.isBlank();
    }
}
