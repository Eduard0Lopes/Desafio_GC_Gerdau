package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoFornecedor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoFornecedorRepository extends MongoRepository<CodigoFornecedor, String> {

    @Query(value = "{ $or: [ { 'nomeNormalizado': { $regex: ?0, $options: 'i' } },"
            + " { 'nome': { $regex: ?0, $options: 'i' } },"
            + " { '_id': { $regex: ?0, $options: 'i' } } ] }")
    List<CodigoFornecedor> buscarPorTexto(String termo);

    List<CodigoFornecedor> findByNomeNormalizado(String nomeNormalizado);
}
