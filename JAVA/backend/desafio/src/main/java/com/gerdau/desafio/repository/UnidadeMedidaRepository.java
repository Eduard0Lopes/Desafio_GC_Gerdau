package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.UnidadeMedida;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnidadeMedidaRepository extends MongoRepository<UnidadeMedida, String> {
}