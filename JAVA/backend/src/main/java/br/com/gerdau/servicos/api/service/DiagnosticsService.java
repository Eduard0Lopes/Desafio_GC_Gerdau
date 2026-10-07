package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.config.AppProperties;
import br.com.gerdau.servicos.api.config.AppProperties.Engine;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.AdminDtos.ColecaoInfo;
import br.com.gerdau.servicos.api.dto.AdminDtos.DiagnosticoResponse;
import br.com.gerdau.servicos.api.persistence.CodigoCompletoRepository;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** "Estou realmente conectado ao banco certo?" — mostra banco, coleções, contagens e índices de busca. */
@Service
public class DiagnosticsService {

    private final MongoTemplate mongo;
    private final AppProperties props;

    public DiagnosticsService(MongoTemplate mongo, AppProperties props) {
        this.mongo = mongo;
        this.props = props;
    }

    public DiagnosticoResponse snapshot() {
        List<ColecaoInfo> colecoes = new ArrayList<>();
        for (TipoComponente t : TipoComponente.values()) {
            colecoes.add(info(t.collection(), props.search().engine() == Engine.ATLAS));
        }
        colecoes.add(info(CodigoCompletoRepository.COLLECTION, false));
        return new DiagnosticoResponse(mongo.getDb().getName(), props.search().engine().name(),
                props.similarity().enforcement().name(), colecoes);
    }

    private ColecaoInfo info(String nome, boolean listarIndicesBusca) {
        boolean existe = mongo.collectionExists(nome);
        Long docs = existe ? mongo.getCollection(nome).estimatedDocumentCount() : null;
        List<String> indices = List.of();
        if (existe && listarIndicesBusca) {
            indices = searchIndexes(nome);
        }
        return new ColecaoInfo(nome, existe, docs, indices);
    }

    private List<String> searchIndexes(String collection) {
        List<String> out = new ArrayList<>();
        try {
            for (Document d : mongo.getCollection(collection)
                    .aggregate(List.of(new Document("$listSearchIndexes", new Document())))) {
                boolean queryable = Boolean.TRUE.equals(d.getBoolean("queryable"));
                out.add(d.getString("name") + (queryable ? " (pronto)" : " (construindo)"));
            }
        } catch (Exception e) {
            out.add("erro ao listar índices de busca: " + e.getMessage());
        }
        return out;
    }
}
