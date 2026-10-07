package br.com.gerdau.servicos.api.persistence;

import br.com.gerdau.servicos.api.domain.ExcecaoSimilaridade;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExcecaoSimilaridadeRepository extends MongoRepository<ExcecaoSimilaridade, String> {
}
