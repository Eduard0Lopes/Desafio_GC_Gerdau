package com.gerdau.desafio.seed;

import com.gerdau.desafio.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Order(10)
public class SchemaBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaBackfill.class);

    private final MongoTemplate mongoTemplate;

    public SchemaBackfill(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            long ajustados = 0;
            ajustados += completar(GrupoServico.class);
            ajustados += completar(CodigoFornecedor.class);
            ajustados += completar(CodigoServico.class);
            ajustados += completar(UnidadeMedida.class);
        ajustados += completarStatus();
            ajustados += classificarNaturezas();
            ajustados += marcarExcecoesAprovadas();
            ajustados += limparComponentesOrfaos();
            if (ajustados > 0) {
                log.info("[SCHEMA] Backfill executado em {} documento(s) (campos base + natureza + exceções).", ajustados);
            }
        } catch (Exception e) {
            log.warn("[SCHEMA] Backfill não executado agora ({}).", e.getMessage());
        }
    }

    /**
     * Marca como "Exceção aprovada" quem foi aprovado apesar da similaridade:
     * 1) novos cadastros da aplicação aprovados (ou criados) com justificativa;
     * 2) itens do histórico aceitos na governança ("Aceitar" uma possível repetição).
     * Itens migrados sem decisão humana ficam de fora: a análise veio da planilha.
     */
    private long marcarExcecoesAprovadas() {
        long ajustados = 0;

        Query novos = new Query(new Criteria().andOperator(
                Criteria.where("origem").is("APLICACAO"),
                Criteria.where("status").is(CodigoCompleto.STATUS_ATIVO),
                Criteria.where("decisaoSimilaridade").ne(null),
                Criteria.where("decisaoSimilaridade").ne("PERMITIR")));
        for (CodigoCompleto cc : mongoTemplate.find(novos, CodigoCompleto.class)) {
            boolean decididoPorHumano = cc.getAprovadoPor() != null
                    || (cc.getJustificativa() != null && !cc.getJustificativa().isBlank());
            if (!decididoPorHumano || cc.getCsId() == null) {
                continue;
            }
            String motivo = cc.getObservacaoAprovacao() != null && !cc.getObservacaoAprovacao().isBlank()
                    ? cc.getObservacaoAprovacao() : cc.getJustificativa();
            if (marcar(cc.getCsId(), motivo)) {
                ajustados++;
            }
        }

        Query aceitos = new Query(new Criteria().andOperator(
                Criteria.where("statusRevisao").is(MatrizDePara.STATUS_REVISADO),
                Criteria.where("reviewedBy").ne(null),
                new Criteria().orOperator(
                        Criteria.where("decision").is("DUPLICIDADE"),
                        Criteria.where("decision").is("ALERTA"))));
        for (MatrizDePara doc : mongoTemplate.find(aceitos, MatrizDePara.class)) {
            String observacao = doc.getObservacaoRevisao() == null ? "" : doc.getObservacaoRevisao().trim();
            if (!observacao.startsWith("MANTER") || doc.getNewId() == null) {
                continue;
            }
            String motivo = observacao.replaceFirst("^MANTER\\s*[—-]\\s*", "").trim();
            if (motivo.isEmpty()) {
                motivo = "Aceito na governança como possível repetição.";
            }
            if (marcar(doc.getNewId(), motivo)) {
                ajustados++;
            }
        }
        return ajustados;
    }

    private boolean marcar(String csId, String motivo) {
        CodigoServico cs = mongoTemplate.findById(csId, CodigoServico.class);
        if (cs == null || Boolean.TRUE.equals(cs.getCasoIsolado())) {
            return false;
        }
        cs.setCasoIsolado(true);
        cs.setMotivoCasoIsolado(motivo);
        cs.setUpdatedAt(Instant.now());
        mongoTemplate.save(cs);
        return true;
    }

    /**
     * Componentes que ainda estão "em validação" mas não pertencem a nenhum pedido vivo
     * (os pedidos foram cancelados/rejeitados) não podem ficar em limbo: saem de cena.
     */
    private long limparComponentesOrfaos() {
        long ajustados = 0;
        for (GrupoServico g : mongoTemplate.find(pendentes(), GrupoServico.class)) {
            if (semPedidoVivo("gsId", g.getId())) {
                g.setStatus(StatusComponente.DESATIVADO);
                g.setUpdatedAt(Instant.now());
                mongoTemplate.save(g);
                ajustados++;
            }
        }
        for (CodigoFornecedor f : mongoTemplate.find(pendentes(), CodigoFornecedor.class)) {
            if (semPedidoVivo("cfId", f.getId())) {
                f.setStatus(StatusComponente.DESATIVADO);
                f.setUpdatedAt(Instant.now());
                mongoTemplate.save(f);
                ajustados++;
            }
        }
        for (CodigoServico cs : mongoTemplate.find(pendentes(), CodigoServico.class)) {
            if (semPedidoVivo("csId", cs.getId())) {
                cs.setStatus(StatusComponente.DESATIVADO);
                cs.setUpdatedAt(Instant.now());
                mongoTemplate.save(cs);
                ajustados++;
            }
        }
        for (UnidadeMedida u : mongoTemplate.find(pendentes(), UnidadeMedida.class)) {
            if (semPedidoVivo("umId", u.getId())) {
                u.setStatus(StatusComponente.DESATIVADO);
                u.setUpdatedAt(Instant.now());
                mongoTemplate.save(u);
                ajustados++;
            }
        }
        return ajustados;
    }

    private Query pendentes() {
        return new Query(Criteria.where("status").is(StatusComponente.PENDENTE_VALIDACAO));
    }

    private boolean semPedidoVivo(String campoVinculo, String id) {
        return mongoTemplate.count(new Query(new Criteria().andOperator(
                Criteria.where(campoVinculo).is(id),
                new Criteria().where("status").nin(
                        CodigoCompleto.STATUS_CANCELADO, CodigoCompleto.STATUS_REJEITADO))),
                CodigoCompleto.class) == 0;
    }

    private long classificarNaturezas() {
        long ajustados = 0;
        for (MatrizDePara doc : mongoTemplate.find(
                new Query(Criteria.where("natureza").is(null)), MatrizDePara.class)) {
            doc.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(doc.getDescricaoCanonica()));
            mongoTemplate.save(doc);
            ajustados++;
        }
        for (CodigoServico cs : mongoTemplate.find(
                new Query(Criteria.where("natureza").is(null)), CodigoServico.class)) {
            cs.setNatureza(com.gerdau.desafio.util.NaturezaUtil.classificar(cs.getDescricao()));
            mongoTemplate.save(cs);
            ajustados++;
        }
        return ajustados;
    }

    private long completarStatus() {
        Query semStatus = new Query(new Criteria().orOperator(
                Criteria.where("status").exists(false),
                Criteria.where("status").is(null)));
        Update atualizacao = new Update().set("status", com.gerdau.desafio.model.StatusComponente.ATIVO);
        long total = 0;
        for (Class<?> entidade : new Class<?>[]{ GrupoServico.class, CodigoFornecedor.class,
                CodigoServico.class, UnidadeMedida.class }) {
            total += mongoTemplate.updateMulti(semStatus, atualizacao, entidade).getModifiedCount();
        }
        return total;
    }

    private long completar(Class<?> entidade) {
        Query semCampos = new Query(new Criteria().andOperator(
                new Criteria().orOperator(
                        Criteria.where("active").exists(false),
                        Criteria.where("active").is(null)),
                new Criteria().orOperator(
                        Criteria.where("createdAt").exists(false),
                        Criteria.where("createdAt").is(null))));
        Update atualizacao = new Update()
                .set("active", true)
                .set("createdAt", Instant.now())
                .set("updatedAt", Instant.now());
        return mongoTemplate.updateMulti(semCampos, atualizacao, entidade).getModifiedCount();
    }
}
