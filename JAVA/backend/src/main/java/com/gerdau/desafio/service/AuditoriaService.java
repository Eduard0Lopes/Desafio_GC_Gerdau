package com.gerdau.desafio.service;

import com.gerdau.desafio.config.CorrelationIdFilter;
import com.gerdau.desafio.model.EventoAuditoria;
import com.gerdau.desafio.repository.EventoAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final EventoAuditoriaRepository repository;

    public AuditoriaService(EventoAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(String acao, String entityType, String entityId, String decisao,
                          String motivo, List<String> candidateIds, List<Double> scores, String actorId) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setActorId(actorId);
        evento.setAction(acao);
        evento.setEntityType(entityType);
        evento.setEntityId(entityId);
        evento.setOccurredAt(Instant.now());
        evento.setDecision(decisao);
        evento.setReason(motivo);
        evento.setCandidateIds(candidateIds == null ? List.of() : candidateIds);
        evento.setSimilarityScores(scores == null ? List.of() : scores);
        evento.setCorrelationId(correlationIdAtual());
        repository.save(evento);
        log.info("[AUDITORIA] acao={} entidade={} id={} decisao={} ator={} corr={}",
                acao, entityType, entityId, decisao, actorId, evento.getCorrelationId());
    }

    public static String correlationIdAtual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object id = attrs.getRequest().getAttribute(CorrelationIdFilter.ATRIBUTO);
                return id == null ? "" : id.toString();
            }
        } catch (Exception ignored) {

        }
        return "";
    }

    public static String usuarioAtual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String usuario = attrs.getRequest()
                        .getHeader(com.gerdau.desafio.config.AutorizacaoInterceptor.HEADER_USUARIO);
                return usuario == null ? "" : usuario.trim();
            }
        } catch (Exception ignored) {

        }
        return "";
    }

    public static String papelAtual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String papel = attrs.getRequest()
                        .getHeader(com.gerdau.desafio.config.AutorizacaoInterceptor.HEADER_PAPEL);
                return papel == null ? "" : papel.trim().toUpperCase(java.util.Locale.ROOT);
            }
        } catch (Exception ignored) {

        }
        return "";
    }

    public static boolean ehAdmin() {
        return com.gerdau.desafio.config.AutorizacaoInterceptor.PAPEL_ADMIN.equalsIgnoreCase(papelAtual());
    }
}
