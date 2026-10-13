package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.CodigoFornecedorRequest;
import com.gerdau.desafio.dto.response.CodigoFornecedorResponse;
import com.gerdau.desafio.service.CodigoFornecedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigos-fornecedor")
public class CodigoFornecedorController {

    private final CodigoFornecedorService service;

    public CodigoFornecedorController(CodigoFornecedorService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CodigoFornecedorResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoFornecedorResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<CodigoFornecedorResponse> salvar(
            @Valid @RequestBody CodigoFornecedorRequest request) {
        CodigoFornecedorResponse salvo = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}