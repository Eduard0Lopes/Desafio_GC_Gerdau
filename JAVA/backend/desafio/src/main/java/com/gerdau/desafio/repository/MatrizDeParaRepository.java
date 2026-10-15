package com.gerdau.desafio.repository;

import com.gerdau.desafio.model.MatrizDePara;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatrizDeParaRepository extends MongoRepository<MatrizDePara, String> {

    List<MatrizDePara> findByStatusRevisaoOrderByScoreDesc(String statusRevisao, Pageable pageable);

    List<MatrizDePara> findByDecisionAndStatusRevisaoOrderByScoreDesc(String decision, String statusRevisao, Pageable pageable);

    long countByStatusRevisao(String statusRevisao);

    long countByDecision(String decision);

    List<MatrizDePara> findByNewId(String newId);

    List<MatrizDePara> findByLegacyDataLegacyId(String legacyId);

    long countByGrupoServico(String grupoServico);

    long countByDecisionAndStatusRevisao(String decision, String statusRevisao);

    long countByLegacyDataQuantidadeCaracteresLessThan(int quantidadeCaracteres);

    long countByLegacyDataQuantidadeCaracteresBetween(int minimo, int maximo);

    long countByLegacyDataQuantidadeCaracteresGreaterThanEqual(int quantidadeCaracteres);

    @Query(value = "{ $or: [ { 'legacyData.legacyId': { $regex: ?0, $options: 'i' } },"
            + " { 'legacyData.legacyDescription': { $regex: ?0, $options: 'i' } },"
            + " { 'descricaoCanonica': { $regex: ?0, $options: 'i' } },"
            + " { 'newId': { $regex: ?0, $options: 'i' } } ] }")
    List<MatrizDePara> buscarPorTexto(String termo, Pageable pageable);
}
