package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.UnidadeMedida;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnidadeMedidaRepository extends MongoRepository<UnidadeMedida, String> {

    @Query(value = "{ $or: [ { 'nomeNormalizado': { $regex: ?0, $options: 'i' } },"
            + " { 'nome': { $regex: ?0, $options: 'i' } },"
            + " { 'sigla': { $regex: ?0, $options: 'i' } } ] }")
    List<UnidadeMedida> buscarPorTexto(String termo);
}
