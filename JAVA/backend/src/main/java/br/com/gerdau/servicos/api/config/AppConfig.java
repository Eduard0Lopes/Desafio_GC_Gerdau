package br.com.gerdau.servicos.api.config;

import br.com.gerdau.servicos.api.domain.SimilarityPolicy;
import br.com.gerdau.servicos.api.text.IdFormatter;
import br.com.gerdau.servicos.api.text.SimilarityScorer;
import br.com.gerdau.servicos.api.text.TextNormalizer;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TextNormalizer textNormalizer() throws IOException {
        try (InputStream in = new ClassPathResource("abbreviations.properties").getInputStream()) {
            return TextNormalizer.fromProperties(in);
        }
    }

    @Bean
    SimilarityPolicy similarityPolicy(AppProperties props) {
        return new SimilarityPolicy(props.similarity().alertThreshold(), props.similarity().blockThreshold());
    }

    @Bean
    SimilarityScorer similarityScorer(TextNormalizer normalizer, SimilarityPolicy policy) {
        // Números diferentes nunca levam ao bloqueio automático (ficam no máximo em "alerta").
        return new SimilarityScorer(normalizer, policy.blockThreshold() - 0.01);
    }

    @Bean
    IdFormatter idFormatter(AppProperties props) {
        AppProperties.Ids ids = props.ids();
        return new IdFormatter(ids.gsWidth(), ids.cfWidth(), ids.csWidth(), ids.umWidth());
    }

    /** Timeouts explícitos: falha rápida (e mensagem clara) quando o banco não responde. */
    @Bean
    MongoClientSettingsBuilderCustomizer mongoClientCustomizer() {
        return builder -> builder
                .applicationName("gerdau-servicos-api")
                .applyToClusterSettings(c -> c.serverSelectionTimeout(10, TimeUnit.SECONDS))
                .applyToSocketSettings(s -> s.connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS));
    }
}
