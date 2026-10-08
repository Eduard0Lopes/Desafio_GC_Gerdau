package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.GrupoServico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrupoServicoRepository extends MongoRepository<GrupoServico, String> {
}