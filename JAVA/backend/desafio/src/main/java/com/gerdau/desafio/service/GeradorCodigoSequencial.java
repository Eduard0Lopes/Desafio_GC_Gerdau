package com.gerdau.desafio.service;

import com.gerdau.desafio.exception.BusinessException;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

/**
 * Gera IDs numéricos com zeros à esquerda no mesmo formato da migração
 * (ex.: "001", "01", "000001") a partir do maior ID já existente.
 */
@Component
public class GeradorCodigoSequencial {

    private static final int TENTATIVAS = 3;

    private final MongoTemplate mongoTemplate;

    public GeradorCodigoSequencial(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public String proximo(String colecao, int tamanho) {
        Query query = new Query(Criteria.where("_id").regex("^\\d{" + tamanho + "}$"))
                .with(Sort.by(Sort.Direction.DESC, "_id"))
                .limit(1);
        query.fields().include("_id");
        Document ultimo = mongoTemplate.findOne(query, Document.class, colecao);

        long proximo = ultimo == null ? 1 : Long.parseLong(ultimo.getString("_id")) + 1;
        if (String.valueOf(proximo).length() > tamanho) {
            throw new BusinessException("Limite de códigos atingido para a coleção " + colecao + ".");
        }
        return String.format("%0" + tamanho + "d", proximo);
    }

    /**
     * Insere a entidade com um novo ID sequencial, repetindo em caso de corrida entre requisições.
     */
    public <T> T inserirComNovoId(T entidade, String colecao, int tamanho, BiConsumer<T, String> definirId) {
        for (int tentativa = 1; ; tentativa++) {
            definirId.accept(entidade, proximo(colecao, tamanho));
            try {
                return mongoTemplate.insert(entidade, colecao);
            } catch (DuplicateKeyException e) {
                if (tentativa >= TENTATIVAS) {
                    throw e;
                }
            }
        }
    }
}
