package com.gerdau.desafio.config;

import com.gerdau.desafio.similarity.RankingPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimilaridadeConfig {

    @Bean
    public RankingPolicy rankingPolicy(
            @Value("${similaridade.limiar-duplicidade:0.88}") double limiarDuplicidade,
            @Value("${similaridade.limiar-alerta:0.70}") double limiarAlerta) {
        return new RankingPolicy(limiarDuplicidade, limiarAlerta);
    }
}
