package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.CodigoCompleto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoCompletoRepository extends MongoRepository<CodigoCompleto, String> {

    boolean existsByIdCompleto(String idCompleto);

    CodigoCompleto findByIdempotencyKey(String idempotencyKey);

    List<CodigoCompleto> findByIdCompletoOrderByCreatedAtDesc(String idCompleto);

    List<CodigoCompleto> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);

    long countByGsId(String gsId);

    long countByCfId(String cfId);

    long countByCsId(String csId);

    long countByUmId(String umId);

    List<CodigoCompleto> findByGsId(String gsId);

    List<CodigoCompleto> findByCfId(String cfId);

    List<CodigoCompleto> findByCsId(String csId);

    List<CodigoCompleto> findByUmId(String umId);

    @Query(value = "{ 'gsId': ?0, 'id': { $ne: ?1 }, 'status': { $nin: ['CANCELADO', 'REJEITADO'] } }", count = true)
    long contarOutrosGs(String gsId, String ignorarId);

    @Query(value = "{ 'cfId': ?0, 'id': { $ne: ?1 }, 'status': { $nin: ['CANCELADO', 'REJEITADO'] } }", count = true)
    long contarOutrosCf(String cfId, String ignorarId);

    @Query(value = "{ 'csId': ?0, 'id': { $ne: ?1 }, 'status': { $nin: ['CANCELADO', 'REJEITADO'] } }", count = true)
    long contarOutrosCs(String csId, String ignorarId);

    @Query(value = "{ 'umId': ?0, 'id': { $ne: ?1 }, 'status': { $nin: ['CANCELADO', 'REJEITADO'] } }", count = true)
    long contarOutrosUm(String umId, String ignorarId);
}
