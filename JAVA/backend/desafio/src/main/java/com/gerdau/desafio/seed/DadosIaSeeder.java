package com.gerdau.desafio.seed;

import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.UnidadeMedida;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Importa os JSONs gerados por {@code IA/src/migration.py} para o MongoDB.
 * Ligado com {@code app.seed.enabled=true}; só popula coleções vazias.
 */
@Component
@Order(0)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DadosIaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIaSeeder.class);
    private static final List<String> COLECOES = List.of(
            GrupoServico.COLECAO,
            UnidadeMedida.COLECAO,
            CodigoFornecedor.COLECAO,
            CodigoServico.COLECAO,
            CodigoCompleto.COLECAO
    );

    private final MongoTemplate mongoTemplate;
    private final Path diretorio;

    public DadosIaSeeder(MongoTemplate mongoTemplate,
                         @Value("${app.seed.dir:../../../IA/data/processed/nosql}") String diretorio) {
        this.mongoTemplate = mongoTemplate;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Files.isDirectory(diretorio)) {
            log.warn("[SEED] Diretório {} não encontrado. Ajuste app.seed.dir.", diretorio);
            return;
        }
        COLECOES.forEach(this::importar);
    }

    private void importar(String colecao) {
        Path arquivo = diretorio.resolve(colecao + ".json");
        if (!Files.exists(arquivo)) {
            log.warn("[SEED] {} ausente, coleção {} ignorada.", arquivo, colecao);
            return;
        }
        if (mongoTemplate.getCollection(colecao).estimatedDocumentCount() > 0) {
            log.info("[SEED] Coleção {} já possui dados, importação ignorada.", colecao);
            return;
        }
        List<Document> documentos = ler(arquivo);
        if (!documentos.isEmpty()) {
            mongoTemplate.getCollection(colecao).insertMany(documentos);
        }
        log.info("[SEED] {} documentos importados em {}.", documentos.size(), colecao);
    }

    private static List<Document> ler(Path arquivo) {
        try {
            String json = Files.readString(arquivo, StandardCharsets.UTF_8);
            return Document.parse("{\"itens\":" + json + "}").getList("itens", Document.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + arquivo, e);
        }
    }
}
