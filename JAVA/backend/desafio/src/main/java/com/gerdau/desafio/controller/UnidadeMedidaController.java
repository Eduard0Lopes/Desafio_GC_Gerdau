package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.UnidadeMedidaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades-medida")
@CrossOrigin(origins = "*")
public class UnidadeMedidaController {

    private final UnidadeMedidaRepository repository;

    // Injeção de dependência via construtor
    public UnidadeMedidaController(UnidadeMedidaRepository repository) {
        this.repository = repository;
    }

    // Endpoint para buscar todos os registros
    @GetMapping
    public List<UnidadeMedida> listarTodos() {
        return repository.findAll();
    }

    // Endpoint para buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<UnidadeMedida> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}