package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoServico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoServicoRepository extends MongoRepository<CodigoServico, String> {

    @Query(value = "{ $or: [ { 'descricao': { $regex: ?0, $options: 'i' } },"
            + " { 'descricaoNormalizada': { $regex: ?0, $options: 'i' } },"
            + " { 'id': { $regex: ?0, $options: 'i' } },"
            + " { 'legacyIds': { $regex: ?0, $options: 'i' } } ] }")
    List<CodigoServico> buscarPorTexto(String termo);

    List<CodigoServico> findByDescricaoNormalizada(String descricaoNormalizada);
}
