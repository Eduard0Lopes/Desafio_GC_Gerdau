package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoCompleto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoCompletoRepository extends MongoRepository<CodigoCompleto, String> {
}