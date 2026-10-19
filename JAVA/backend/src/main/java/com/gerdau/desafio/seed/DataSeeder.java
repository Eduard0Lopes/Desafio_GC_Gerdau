package com.gerdau.desafio.seed;

import com.gerdau.desafio.model.*;
import com.gerdau.desafio.repository.*;
import com.gerdau.desafio.util.TextoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final Map<String, String> NOMES_UNIDADES = Map.ofEntries(
            Map.entry("SV", "Serviço"), Map.entry("KG", "Quilograma"), Map.entry("H", "Hora"),
            Map.entry("M3", "Metro cúbico"), Map.entry("M2", "Metro quadrado"), Map.entry("CM", "Centímetro"),
            Map.entry("M", "Metro"), Map.entry("ST", "Unidade (peça)"), Map.entry("MON", "Mês"),
            Map.entry("L", "Litro"), Map.entry("TO", "Tonelada"), Map.entry("TAG", "Dia"),
            Map.entry("MM", "Milímetro"), Map.entry("ZCJ", "Conjunto"), Map.entry("ML", "Mililitro"),
            Map.entry("CDM", "Centímetro cúbico"), Map.entry("KM", "Quilômetro"));

    private final MatrizDeParaRepository matrizDeParaRepository;
    private final GrupoServicoRepository grupoServicoRepository;
    private final CodigoFornecedorRepository codigoFornecedorRepository;
    private final CodigoServicoRepository codigoServicoRepository;
    private final UnidadeMedidaRepository unidadeMedidaRepository;
    private final CodigoCompletoRepository codigoCompletoRepository;

    private final boolean seedHabilitado;
    private final String diretorioPlanilhas;

    public DataSeeder(MatrizDeParaRepository matrizDeParaRepository,
                      GrupoServicoRepository grupoServicoRepository,
                      CodigoFornecedorRepository codigoFornecedorRepository,
                      CodigoServicoRepository codigoServicoRepository,
                      UnidadeMedidaRepository unidadeMedidaRepository,
                      CodigoCompletoRepository codigoCompletoRepository,
                      @Value("${app.seed.enabled:true}") boolean seedHabilitado,
                      @Value("${app.seed.planilhas:}") String diretorioPlanilhas) {
        this.matrizDeParaRepository = matrizDeParaRepository;
        this.grupoServicoRepository = grupoServicoRepository;
        this.codigoFornecedorRepository = codigoFornecedorRepository;
        this.codigoServicoRepository = codigoServicoRepository;
        this.unidadeMedidaRepository = unidadeMedidaRepository;
        this.codigoCompletoRepository = codigoCompletoRepository;
        this.seedHabilitado = seedHabilitado;
        this.diretorioPlanilhas = diretorioPlanilhas;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            executar();
        } catch (Exception e) {
            log.warn("[SEED] Não foi possível carregar os dados oficiais agora ({}). "
                    + "A aplicação continua no ar e a carga acontece na próxima subida.", e.getMessage());
        }
    }

    private void executar() throws Exception {
        if (!seedHabilitado) {
            log.info("[SEED] Carga desabilitada (app.seed.enabled=false).");
            return;
        }
        
        long total = matrizDeParaRepository.count();
        boolean formatoAtual = total > 0
                && matrizDeParaRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 5))
                        .getContent().stream()
                        .allMatch(m -> m.getLegacyData() != null && m.getStatusRevisao() != null);
        if (formatoAtual) {
            log.info("[SEED] Dados oficiais já carregados ({} documentos) — nada a fazer.", total);
            return;
        }

        Path diretorio = localizarDiretorio();
        Path baseSaneada = diretorio.resolve("base_saneada.xlsx");
        Path matriz = diretorio.resolve("matriz_de_para_gerdau.xlsx");
        if (!Files.exists(baseSaneada) || !Files.exists(matriz)) {
            log.warn("[SEED] Planilhas oficiais não encontradas em {} — carga ignorada.", diretorio);
            return;
        }

        log.info("[SEED] Preparando o estado oficial (coleções recriadas)...");
        matrizDeParaRepository.deleteAll();
        grupoServicoRepository.deleteAll();
        codigoFornecedorRepository.deleteAll();
        codigoServicoRepository.deleteAll();
        unidadeMedidaRepository.deleteAll();
        codigoCompletoRepository.deleteAll();

        log.info("[SEED] Lendo planilhas oficiais em {} ...", diretorio);
        List<Map<String, String>> base = PlanilhaOficial.ler(baseSaneada);
        List<Map<String, String>> dePara = PlanilhaOficial.ler(matriz);
        log.info("[SEED] base_saneada: {} linhas | matriz_de_para: {} linhas", base.size(), dePara.size());

        Map<String, Map<String, String>> basePorLegacy = new HashMap<>();
        for (Map<String, String> linha : base) {
            basePorLegacy.put(linha.getOrDefault("Código do Serviço", ""), linha);
        }

        List<MatrizDePara> mesclados = new ArrayList<>();
        Map<String, List<String>> legadosPorNewId = new LinkedHashMap<>();
        Map<String, String> nomeGrupoPorChave = new LinkedHashMap<>();
        Map<String, String> siglaPorChave = new LinkedHashMap<>();
        Map<String, Long> fornecedorPorChave = new LinkedHashMap<>();
        Map<String, String> descricaoPorNewId = new LinkedHashMap<>();

        for (Map<String, String> m : dePara) {
            String legacyId = m.getOrDefault("legacyId", "");
            Map<String, String> b = basePorLegacy.getOrDefault(legacyId, Map.of());

            MatrizDePara doc = new MatrizDePara();
            doc.setNewId(m.getOrDefault("newId", ""));
            doc.setDescricaoCanonica(m.getOrDefault("descricaoCanonica", ""));
            doc.setGrupoServico(b.getOrDefault("Grupo de Serviço", m.getOrDefault("grupoServico", "")));
            doc.setUnidadeMedida(b.getOrDefault("Unidade medida", m.getOrDefault("unidadeMedida", "")));
            doc.setDecision(m.getOrDefault("decision", ""));
            doc.setScore(numero(m.getOrDefault("score", "0")));
            doc.setReason(m.getOrDefault("reason", ""));
            doc.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(m.getOrDefault("descricaoCanonica", "")));

            MatrizDePara.LegacyData legado = new MatrizDePara.LegacyData();
            legado.setLegacyId(legacyId);
            legado.setLegacyDescription(m.getOrDefault("legacyDescription", ""));
            legado.setLegacySource(m.getOrDefault("legacySource", "ARQUIVO_SAP_GERDAU"));
            legado.setCodigoFornecedor(numeroLong(b.getOrDefault("Código do Fornecedor", "")));
            legado.setAlertaSap(Boolean.parseBoolean(b.getOrDefault("AlertaSAP", "false")));
            legado.setQuantidadeCaracteres((int) Math.round(numero(b.getOrDefault("QuantidadeCaracteres", "0"))));
            doc.setLegacyData(legado);

            String decisao = doc.getDecision();
            boolean pendente = "DUPLICIDADE".equals(decisao) || "ALERTA".equals(decisao);
            doc.setStatusRevisao(pendente ? MatrizDePara.STATUS_PENDENTE : MatrizDePara.STATUS_SEM_PENDENCIA);

            mesclados.add(doc);
            legadosPorNewId.computeIfAbsent(doc.getNewId(), k -> new ArrayList<>()).add(legacyId);
            descricaoPorNewId.putIfAbsent(doc.getNewId(), doc.getDescricaoCanonica());
            nomeGrupoPorChave.putIfAbsent(doc.getGrupoServico(), doc.getGrupoServico());
            siglaPorChave.putIfAbsent(doc.getUnidadeMedida(), doc.getUnidadeMedida());
            if (legado.getCodigoFornecedor() != null) {
                fornecedorPorChave.putIfAbsent(String.valueOf(legado.getCodigoFornecedor()), legado.getCodigoFornecedor());
            }
        }
        matrizDeParaRepository.saveAll(mesclados);
        log.info("[SEED] matriz_depara (mesclada): {} documentos", mesclados.size());

        List<GrupoServico> grupos = new ArrayList<>();
        for (String nome : nomeGrupoPorChave.keySet()) {
            GrupoServico g = new GrupoServico();
            g.setId(slug(nome));
            g.setNome(nome);
            g.setNomeNormalizado(TextoUtil.normalizar(nome));
            g.setOrigem("MIGRACAO");
            grupos.add(g);
        }
        grupoServicoRepository.saveAll(grupos);
        log.info("[SEED] grupos_servico: {}", grupos.size());

        List<UnidadeMedida> unidades = new ArrayList<>();
        for (String sigla : siglaPorChave.keySet()) {
            UnidadeMedida u = new UnidadeMedida();
            u.setId(sigla.toUpperCase());
            u.setSigla(sigla.toUpperCase());
            String nome = NOMES_UNIDADES.getOrDefault(sigla.toUpperCase(), sigla);
            u.setNome(nome);
            u.setNomeNormalizado(TextoUtil.normalizar(nome));
            u.setOrigem("MIGRACAO");
            unidades.add(u);
        }
        unidadeMedidaRepository.saveAll(unidades);
        log.info("[SEED] unidades_medida: {}", unidades.size());

        List<CodigoFornecedor> fornecedores = new ArrayList<>();
        for (Map.Entry<String, Long> e : fornecedorPorChave.entrySet()) {
            CodigoFornecedor f = new CodigoFornecedor();
            f.setId(e.getKey());
            f.setCodigoFornecedorSAP(e.getValue());
            f.setNome("Fornecedor " + e.getKey());
            f.setNomeNormalizado(TextoUtil.normalizar("fornecedor " + e.getKey()));
            f.setOrigem("MIGRACAO");
            fornecedores.add(f);
        }
        codigoFornecedorRepository.saveAll(fornecedores);
        log.info("[SEED] codigos_fornecedor: {}", fornecedores.size());

        List<CodigoServico> servicos = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : legadosPorNewId.entrySet()) {
            CodigoServico cs = new CodigoServico();
            cs.setId(e.getKey());
            String descricao = descricaoPorNewId.getOrDefault(e.getKey(), "");
            cs.setDescricao(descricao);
            cs.setDescricaoNormalizada(TextoUtil.normalizar(descricao));
            cs.setQuantidadeCaracteres(descricao.length());
            cs.setOrigem("MIGRACAO");
            cs.setLegacyIds(e.getValue());
            cs.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(descricao));
            cs.setCreatedAt(Instant.now());
            cs.setUpdatedAt(Instant.now());
            servicos.add(cs);
        }
        codigoServicoRepository.saveAll(servicos);
        log.info("[SEED] codigos_servico (canônicos): {}", servicos.size());

        Set<String> vistos = new HashSet<>();
        List<CodigoCompleto> completos = new ArrayList<>();
        for (MatrizDePara doc : mesclados) {
            MatrizDePara.LegacyData l = doc.getLegacyData();
            String gsId = slug(doc.getGrupoServico());
            String cfId = l.getCodigoFornecedor() == null ? "0" : String.valueOf(l.getCodigoFornecedor());
            String csId = doc.getNewId();
            String umId = doc.getUnidadeMedida() == null ? "" : doc.getUnidadeMedida().toUpperCase();
            String idCompleto = gsId + "_" + cfId + "_" + csId + "_" + umId;
            if (!vistos.add(idCompleto)) {
                continue;
            }
            CodigoCompleto cc = new CodigoCompleto();
            cc.setId(idCompleto);
            cc.setIdCompleto(idCompleto);
            cc.setGsId(gsId);
            cc.setCfId(cfId);
            cc.setCsId(csId);
            cc.setUmId(umId);
            cc.setDescricaoSnapshot(doc.getGrupoServico() + " | " + "Fornecedor " + cfId
                    + " | " + doc.getDescricaoCanonica() + " | " + umId);
            cc.setOrigem("MIGRACAO");
            cc.setLegacyIds(List.of(l.getLegacyId()));
            cc.setStatus(CodigoCompleto.STATUS_ATIVO);
            cc.setDecisaoSimilaridade(doc.getDecision());
            cc.setMelhorScore(doc.getScore());
            completos.add(cc);
        }
        codigoCompletoRepository.saveAll(completos);
        log.info("[SEED] codigos_completos (composições distintas): {}", completos.size());

        long pendentes = mesclados.stream()
                .filter(d -> MatrizDePara.STATUS_PENDENTE.equals(d.getStatusRevisao())).count();
        log.info("[SEED] CONCLUÍDO — {} registros legados mapeados | {} pendentes de revisão na governança.",
                mesclados.size(), pendentes);
    }

    private Path localizarDiretorio() {
        List<String> candidatos = new ArrayList<>();
        if (diretorioPlanilhas != null && !diretorioPlanilhas.isBlank()) {
            candidatos.add(diretorioPlanilhas);
        }
        candidatos.addAll(List.of(
                "IA/data/processed",
                "../IA/data/processed",
                "../../IA/data/processed",
                "../../../IA/data/processed",
                "data/processed"));
        for (String c : candidatos) {
            Path p = Path.of(c);
            if (Files.exists(p.resolve("base_saneada.xlsx"))) {
                return p.toAbsolutePath().normalize();
            }
        }
        return Path.of(candidatos.get(0)).toAbsolutePath().normalize();
    }

    public static String slug(String nome) {
        return TextoUtil.normalizar(nome == null ? "" : nome).replace(" ", "_").toUpperCase();
    }

    private static double numero(String valor) {
        try {
            return Double.parseDouble(valor.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static Long numeroLong(String valor) {
        try {
            return (long) Double.parseDouble(valor.replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }
}
