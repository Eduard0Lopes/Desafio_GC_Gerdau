package com.gerdau.desafio.config;

import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.UnidadeMedida;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Cria os índices usados pelas consultas e pelas validações de unicidade.
 * Os índices não são únicos para tolerar dados legados importados com repetições.
 */
@Component
@Order(1)
public class MongoIndexConfig implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        indexar(GrupoServico.COLECAO, "nomeNormalizado");
        indexar(UnidadeMedida.COLECAO, "sigla");
        indexar(CodigoFornecedor.COLECAO, "codigoFornecedorSAP");
        indexar(CodigoServico.COLECAO, "descricaoNormalizada");
        indexar(CodigoServico.COLECAO, "legacyIds");
        for (String campo : new String[]{"gsId", "cfId", "csId", "umId", "legacyIds"}) {
            indexar(CodigoCompleto.COLECAO, campo);
        }
    }

    private void indexar(String colecao, String campo) {
        mongoTemplate.indexOps(colecao).createIndex(new Index().on(campo, Sort.Direction.ASC));
    }
}
