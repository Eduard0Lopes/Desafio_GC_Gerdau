package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.SimilaridadeRequest;
import com.gerdau.desafio.dto.response.SimilaridadeResponse;
import com.gerdau.desafio.service.SimilaridadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/itens/similaridade")
@Tag(name = "Similaridade", description = "Consulta de itens semelhantes — tipo: CS, GS, CF ou UM (seções 6 e 10.1)")
public class SimilaridadeController {

    private final SimilaridadeService service;

    public SimilaridadeController(SimilaridadeService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Consultar similares pelo corpo da requisição")
    public ResponseEntity<SimilaridadeResponse> consultar(@Valid @RequestBody SimilaridadeRequest request) {
        return ResponseEntity.ok(service.consultar(request.descricao(), request.limite(), request.tipo()));
    }

    @GetMapping
    @Operation(summary = "Consultar similares por parâmetro de URL")
    public ResponseEntity<SimilaridadeResponse> consultarPorParametro(
            @RequestParam String descricao,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false, defaultValue = "CS") String tipo) {
        return ResponseEntity.ok(service.consultar(descricao, limite, tipo));
    }
}
