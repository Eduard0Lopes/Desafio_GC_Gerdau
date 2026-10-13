package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.CodigoCompleto;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigos-completo")
@CrossOrigin(origins = "*")
public class CodigoCompletoController {

    private final CodigoCompletoRepository repository;

    public CodigoCompletoController(CodigoCompletoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CodigoCompleto> listarTodos() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoCompleto> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CodigoCompleto> salvar(@RequestBody CodigoCompleto codigoCompleto) {
        if (codigoCompleto.getDescricaoSnapshot() == null || codigoCompleto.getDescricaoSnapshot().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do grupo de serviço é obrigatório.");
        }

        CodigoCompleto salvo = repository.save(codigoCompleto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}