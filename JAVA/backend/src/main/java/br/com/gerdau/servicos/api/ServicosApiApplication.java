package br.com.gerdau.servicos.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ServicosApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicosApiApplication.class, args);
    }
}
