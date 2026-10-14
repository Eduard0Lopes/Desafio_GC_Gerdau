package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoFornecedor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CodigoFornecedorRepository extends MongoRepository<CodigoFornecedor, String> {

    Optional<CodigoFornecedor> findByCodigoFornecedorSAP(Long codigoFornecedorSAP);
}
