package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.UnidadeMedidaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades-medida")
@CrossOrigin(origins = "*")
public class UnidadeMedidaController {

    private final UnidadeMedidaRepository repository;

    public UnidadeMedidaController(UnidadeMedidaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<UnidadeMedida> listarTodos() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeMedida> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UnidadeMedida> salvar(@RequestBody UnidadeMedida unidadeMedida) {
        if (unidadeMedida.getSigla() == null || unidadeMedida.getSigla().trim().isEmpty()) {
            throw new IllegalArgumentException("A sigla da unidade de medida é obrigatória.");
        }

        UnidadeMedida salvo = repository.save(unidadeMedida);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}