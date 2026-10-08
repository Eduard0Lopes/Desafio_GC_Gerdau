package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigosFornecedor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoFornecedorRepository extends MongoRepository<CodigosFornecedor, String> {

}
