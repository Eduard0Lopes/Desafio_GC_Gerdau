package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoServico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoServicoRepository extends MongoRepository<CodigoServico, String> {
}