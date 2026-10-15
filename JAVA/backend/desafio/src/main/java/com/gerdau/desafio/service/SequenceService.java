package com.gerdau.desafio.service;

import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.model.Contador;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Sequência atômica de IDs (especificação, seção 5 — SequenceCounter).
 *
 * Regras:
 *  - incremento atômico (sem corrida, sem ID entregue duas vezes);
 *  - o ID oficial do C.S. tem formato {@code CS-XXXXXX} (faixa 000001–999999);
 *  - os registros migrados usam IDs derivados de hash e podem ocupar qualquer
 *    posição da faixa — por isso o gerador **pula IDs já existentes** (ocupar um
 *    espaço nunca usado não é reutilização; jamais são reentregues IDs já em uso);
 *  - se a faixa inteira se esgotar, retorna erro de domínio explícito —
 *    nunca trunca nem amplia o formato silenciosamente.
 */
@Service
public class SequenceService {

    private static final Logger log = LoggerFactory.getLogger(SequenceService.class);

    public static final String SEQ_CODIGO_SERVICO = "codigos_servico";
    public static final long MAXIMO = 999999L;

    private final MongoTemplate mongoTemplate;
    private final CodigoServicoRepository codigoServicoRepository;

    public SequenceService(MongoTemplate mongoTemplate, CodigoServicoRepository codigoServicoRepository) {
        this.mongoTemplate = mongoTemplate;
        this.codigoServicoRepository = codigoServicoRepository;
    }

    /** Próximo código de serviço livre no formato oficial CS-XXXXXX. */
    public synchronized String proximoCodigoServico() {
        for (int tentativa = 0; tentativa < 100; tentativa++) {
            long valor = proximoIncremento(SEQ_CODIGO_SERVICO);
            if (valor > MAXIMO) {
                throw new BusinessException(
                        "Faixa de IDs da sequência '" + SEQ_CODIGO_SERVICO + "' esgotada (máximo "
                                + MAXIMO + "). Amplie a faixa formalmente — IDs não são truncados nem reutilizados.");
            }
            String id = String.format("CS-%06d", valor);
            if (!codigoServicoRepository.existsById(id)) {
                return id;
            }
            // ID ocupado por registro migrado (hash): segue para o próximo —
            // nunca é reutilizado um ID já existente.
        }
        throw new BusinessException(
                "Não foi possível alocar um novo código de serviço após várias tentativas. "
                        + "Verifique a ocupação da faixa de IDs.");
    }

    /**
     * Incremento atômico da sequência. Um contador antigo alinhado ao maior ID
     * migrado (defeito corrigido) é reajustado para o início do espaço livre.
     */
    private long proximoIncremento(String nome) {
        Contador atual = mongoTemplate.findById(nome, Contador.class);
        if (atual == null) {
            Contador inicial = new Contador();
            inicial.setId(nome);
            inicial.setValor(0L);
            mongoTemplate.save(inicial);
        } else if (atual.getValor() >= MAXIMO) {
            log.warn("[SEQUENCE] Contador '{}' estava em {} (alinhado aos IDs migrados). "
                    + "Reajustando para o início do espaço livre — IDs em uso não são reutilizados.", nome, atual.getValor());
            atual.setValor(0L);
            mongoTemplate.save(atual);
        }

        Contador incrementado = mongoTemplate.findAndModify(
                new Query(Criteria.where("_id").is(nome)),
                new Update().inc("valor", 1),
                new org.springframework.data.mongodb.core.FindAndModifyOptions().returnNew(true),
                Contador.class);
        return incrementado == null ? 1L : incrementado.getValor();
    }
}
