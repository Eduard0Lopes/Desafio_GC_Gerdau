package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.GrupoServico;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GrupoServicoRepository extends MongoRepository<GrupoServico, String> {

    Optional<GrupoServico> findByNomeNormalizado(String nomeNormalizado);
}
