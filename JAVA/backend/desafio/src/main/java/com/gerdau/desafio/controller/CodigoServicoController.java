package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.CodigoServicoRequest;
import com.gerdau.desafio.dto.response.CodigoServicoResponse;
import com.gerdau.desafio.service.CodigoServicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigo-servico")
public class CodigoServicoController {

    private final CodigoServicoService service;

    public CodigoServicoController(CodigoServicoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CodigoServicoResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoServicoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<CodigoServicoResponse> salvar(
            @Valid @RequestBody CodigoServicoRequest request) {
        CodigoServicoResponse salvo = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}