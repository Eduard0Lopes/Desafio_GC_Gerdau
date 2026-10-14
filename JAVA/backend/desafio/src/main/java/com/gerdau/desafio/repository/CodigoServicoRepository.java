package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoServico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CodigoServicoRepository extends MongoRepository<CodigoServico, String> {

    Optional<CodigoServico> findByDescricaoNormalizada(String descricaoNormalizada);

    @Query(value = "{}", fields = "{ 'descricao': 1, 'descricaoNormalizada': 1 }")
    List<CodigoServico> findAllDescricoes();
}
