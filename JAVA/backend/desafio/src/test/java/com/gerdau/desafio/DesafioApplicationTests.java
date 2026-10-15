package com.gerdau.desafio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Carga do contexto sem exigir MongoDB disponível (seed e índices desligados). */
@SpringBootTest(properties = {
        "app.mongo.ensure-indexes=false",
        "app.seed.enabled=false"
})
class DesafioApplicationTests {

    @Test
    void contextLoads() {
    }

}
