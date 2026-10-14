package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.UnidadeMedida;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UnidadeMedidaRepository extends MongoRepository<UnidadeMedida, String> {

    Optional<UnidadeMedida> findBySigla(String sigla);
}
