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

        }
        throw new BusinessException(
                "Não foi possível alocar um novo código de serviço após várias tentativas. "
                        + "Verifique a ocupação da faixa de IDs.");
    }

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
