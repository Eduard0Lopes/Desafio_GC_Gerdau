package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigo-servico")
@CrossOrigin(origins = "*")
public class CodigoServicoController {

    private final CodigoServicoRepository repository;

    // Injeção de dependência via construtor
    public CodigoServicoController(CodigoServicoRepository repository) {
        this.repository = repository;
    }

    // Endpoint para buscar todos os registros
    @GetMapping
    public List<CodigoServico> listarTodos() {
        return repository.findAll();
    }

    // Endpoint para buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<CodigoServico> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}