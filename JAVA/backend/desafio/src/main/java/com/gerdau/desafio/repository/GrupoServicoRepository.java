package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.GrupoServico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoServicoRepository extends MongoRepository<GrupoServico, String> {

    @Query(value = "{ $or: [ { 'nomeNormalizado': { $regex: ?0, $options: 'i' } }, { 'nome': { $regex: ?0, $options: 'i' } } ] }")
    List<GrupoServico> buscarPorTexto(String termo);
}
