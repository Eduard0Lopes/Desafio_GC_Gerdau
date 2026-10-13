package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoServico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoServicoRepository extends MongoRepository<CodigoServico, String> {

    @Query("{ 'descricaoNormalizada' : { $regex: ?0, $options: 'i' } }")
    List<CodigoServico> findByDescricaoNormalizadaRegex(String termo);
}