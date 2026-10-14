package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

@Component
public class ConsultaPaginada {

    private final MongoTemplate mongoTemplate;

    public ConsultaPaginada(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public <T, R> PageResponse<R> paginar(Query query, Class<T> tipo, int page, int size, Function<List<T>, List<R>> mapear) {
        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), tipo);
        query.with(PageRequest.of(page, size, Sort.by("_id")));
        return PageResponse.of(mapear.apply(mongoTemplate.find(query, tipo)), page, size, total);
    }

    public <T> List<T> listar(Query query, Class<T> tipo) {
        return mongoTemplate.find(query.with(Sort.by("_id")), tipo);
    }

    public static Criteria contem(String campo, String termo) {
        return Criteria.where(campo).regex(Pattern.quote(termo), "i");
    }

    public static Criteria comecaCom(String campo, String termo) {
        return Criteria.where(campo).regex("^" + Pattern.quote(termo));
    }
}
