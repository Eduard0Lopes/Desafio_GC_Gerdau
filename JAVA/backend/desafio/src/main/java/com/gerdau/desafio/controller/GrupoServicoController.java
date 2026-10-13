package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.GrupoServicoRequest;
import com.gerdau.desafio.dto.response.GrupoServicoResponse;
import com.gerdau.desafio.service.GrupoServicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos-servico")
public class GrupoServicoController {

    private final GrupoServicoService service;

    public GrupoServicoController(GrupoServicoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GrupoServicoResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoServicoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<GrupoServicoResponse> salvar(
            @Valid @RequestBody GrupoServicoRequest request) {
        GrupoServicoResponse salvo = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}