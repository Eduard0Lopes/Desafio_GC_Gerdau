package br.com.gerdau.servicos.api.persistence;

import br.com.gerdau.servicos.api.domain.SolicitacaoGovernanca;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SolicitacaoGovernancaRepository extends MongoRepository<SolicitacaoGovernanca, String> {

    List<SolicitacaoGovernanca> findByStatusOrderByCreatedAtAsc(SolicitacaoGovernanca.Status status, Pageable pageable);
}
