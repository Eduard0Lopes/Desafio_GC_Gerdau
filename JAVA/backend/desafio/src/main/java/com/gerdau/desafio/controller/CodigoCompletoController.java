package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.CodigoCompletoRequest;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.service.CodigoCompletoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigos-completo")
public class CodigoCompletoController {

    private final CodigoCompletoService service;

    public CodigoCompletoController(CodigoCompletoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CodigoCompletoResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoCompletoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<CodigoCompletoResponse> salvar(
            @Valid @RequestBody CodigoCompletoRequest request) {
        CodigoCompletoResponse salvo = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}