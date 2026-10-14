package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoCompleto;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CodigoCompletoRepository extends MongoRepository<CodigoCompleto, String> {

    List<CodigoCompleto> findByLegacyIdsContaining(String legacyId);

    boolean existsByGsId(String gsId);

    boolean existsByCfId(String cfId);

    boolean existsByCsId(String csId);

    boolean existsByUmId(String umId);
}
