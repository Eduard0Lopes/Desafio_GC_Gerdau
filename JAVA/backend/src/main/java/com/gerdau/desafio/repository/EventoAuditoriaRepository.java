package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.EventoAuditoria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoAuditoriaRepository extends MongoRepository<EventoAuditoria, String> {

    List<EventoAuditoria> findTop100ByOrderByOccurredAtDesc();

    List<EventoAuditoria> findByActionOrderByOccurredAtDesc(String action);

    List<EventoAuditoria> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    long countByAction(String action);
}
