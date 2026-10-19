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

@Service
public class PreviewService {

    public static final String ACAO_CRIAR = "CRIAR";
    public static final String ACAO_ENVIAR_VALIDACAO = "ENVIAR_VALIDACAO";

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
            CodigoServico existente = buscarServicoExistente(descricao);
            if (existente != null) {
                // não pode existir outro código de serviço com o mesmo nome
                cs = existente;
            } else {
                cs = new CodigoServico();
                cs.setId(MARCADOR_NOVO);
                cs.setDescricao(descricao);
                cs.setDescricaoNormalizada(TextoUtil.normalizar(descricao));
                cs.setQuantidadeCaracteres(descricao.length());
                cs.setOrigem("SISTEMA");
                codigoNovo = true;
            }
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

    private GrupoServico buscarGrupoExistente(String nome) {
        GrupoServico porId = grupoServicoRepository.findById(DataSeeder.slug(nome)).orElse(null);
        if (porId != null) {
            return porId;
        }
        return grupoServicoRepository.findByNomeNormalizado(TextoUtil.normalizar(nome))
                .stream().findFirst().orElse(null);
    }

    private CodigoFornecedor buscarFornecedorExistente(String nome, String codigoSap) {
        if (codigoSap != null) {
            return codigoFornecedorRepository.findById(codigoSap).orElse(null);
        }
        return codigoFornecedorRepository.findByNomeNormalizado(TextoUtil.normalizar(nome))
                .stream().findFirst().orElse(null);
    }

    private CodigoServico buscarServicoExistente(String descricao) {
        return codigoServicoRepository.findByDescricaoNormalizada(TextoUtil.normalizar(descricao))
                .stream().findFirst().orElse(null);
    }

    public PreviewResponse montarPreview(PreviewRequest req) {
        ComponentesResolvidos r = resolver(req, false);

        String idCompleto = montarIdCompleto(r);
        boolean todosExistentes = !r.grupoNovo() && !r.fornecedorNovo() && !r.codigoNovo() && !r.unidadeNova();
        // a composição pode ter vários vínculos (criados quantas vezes forem necessárias);
        // para gestão, interessa o pedido pendente mais recente — senão, o vínculo mais novo
        CodigoCompleto existente = todosExistentes
                ? codigoCompletoRepository.findByIdCompletoOrderByCreatedAtDesc(idCompleto).stream()
                        .filter(c -> CodigoCompleto.STATUS_PENDENTE.equals(c.getStatus()))
                        .findFirst()
                        .orElseGet(() -> codigoCompletoRepository.findByIdCompletoOrderByCreatedAtDesc(idCompleto)
                                .stream().findFirst().orElse(null))
                : null;
        // rejeitados/cancelados não contam como vínculo vivo
        boolean jaExiste = existente != null
                && !CodigoCompleto.STATUS_REJEITADO.equals(existente.getStatus())
                && !CodigoCompleto.STATUS_CANCELADO.equals(existente.getStatus());
        String statusExistente = existente == null ? null : existente.getStatus();

        String descricao = r.codigoServico().getDescricao();
        boolean excedeSap = TextoUtil.excedeLimiteSap(descricao);

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
            // informação, não bloqueio: a composição pode ser vinculada quantas vezes
            // forem necessárias — cada envio cria um novo registro
            acaoRecomendada = CodigoCompleto.STATUS_PENDENTE.equals(statusExistente)
                    ? "Já existe um pedido pendente para esta composição — este envio criará mais um vínculo."
                    : "Esta composição já está vinculada — este envio criará mais um vínculo.";
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
                statusExistente,
                status,
                decisao,
                acaoRecomendada,
                melhorScore,
                similares,
                List.copyOf(alertas));
    }

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
                melhor.score(), sim.decisao(), melhor.motivo(), melhor.casoIsolado(), melhor.motivoCasoIsolado()));
    }

    private static int severidade(String decisao) {
        return switch (decisao) {
            case "REVISAR" -> 2;
            case "ALERTAR" -> 1;
            default -> 0;
        };
    }

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
