package com.gerdau.desafio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.mongo.ensure-indexes=false",
        "app.seed.enabled=false"
})
class DesafioApplicationTests {

    @Test
    void contextLoads() {
    }

}
