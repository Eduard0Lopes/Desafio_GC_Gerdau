package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.Excecao;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcecaoRepository extends MongoRepository<Excecao, String> {

    List<Excecao> findTop50ByOrderByDataHoraDesc();
}
