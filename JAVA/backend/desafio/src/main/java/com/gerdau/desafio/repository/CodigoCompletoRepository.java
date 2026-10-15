package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoCompleto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoCompletoRepository extends MongoRepository<CodigoCompleto, String> {

    boolean existsByIdCompleto(String idCompleto);

    CodigoCompleto findByIdempotencyKey(String idempotencyKey);

    List<CodigoCompleto> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
