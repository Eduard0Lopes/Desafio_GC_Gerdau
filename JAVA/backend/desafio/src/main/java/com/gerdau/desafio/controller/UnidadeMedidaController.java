package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.UnidadeMedidaRequest;
import com.gerdau.desafio.dto.response.UnidadeMedidaResponse;
import com.gerdau.desafio.service.UnidadeMedidaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades-medida")
public class UnidadeMedidaController {

    private final UnidadeMedidaService service;

    public UnidadeMedidaController(UnidadeMedidaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UnidadeMedidaResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeMedidaResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UnidadeMedidaResponse> salvar(
            @Valid @RequestBody UnidadeMedidaRequest request) {
        UnidadeMedidaResponse salvo = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}