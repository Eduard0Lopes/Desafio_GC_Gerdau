package com.gerdau.desafio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Disponibilidade e prontidão (especificação, seção 7) — sem expor segredos.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Infraestrutura", description = "Health checks de disponibilidade e prontidão")
public class HealthController {

    private final MongoTemplate mongoTemplate;
    private final String uri;

    public HealthController(MongoTemplate mongoTemplate,
                            @Value("${spring.mongodb.uri:mongodb://localhost:27017/gerdau_prototipo}") String uri) {
        this.mongoTemplate = mongoTemplate;
        this.uri = uri;
    }

    @GetMapping("/health")
    @Operation(summary = "Disponibilidade da aplicação")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "aplicacao", "desafio-gc-gerdau",
                "banco", bancoAlvo(),
                "agora", Instant.now().toString()));
    }

    /**
     * Banco alvo SEM credenciais (ex.: "cluster0.x.mongodb.net/gerdau_prototipo"
     * ou "localhost:27017/gerdau_prototipo") — ajuda a evitar confusão entre
     * Mongo local e Atlas.
     */
    private String bancoAlvo() {
        try {
            String semEsquema = uri.replaceFirst("^mongodb(\\+srv)?://", "");
            String semCredenciais = semEsquema.contains("@")
                    ? semEsquema.substring(semEsquema.indexOf('@') + 1) : semEsquema;
            return semCredenciais.replaceAll("[/?].*$", "").trim();
        } catch (Exception e) {
            return "não identificado";
        }
    }

    @GetMapping("/ready")
    @Operation(summary = "Prontidão (inclui verificação de acesso ao banco)")
    public ResponseEntity<Map<String, Object>> ready() {
        try {
            mongoTemplate.getCollection("codigos_servico").estimatedDocumentCount();
            return ResponseEntity.ok(Map.of("status", "READY", "banco", "UP", "agora", Instant.now().toString()));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "NOT_READY",
                    "banco", "DOWN",
                    "agora", Instant.now().toString()));
        }
    }
}
