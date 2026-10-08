package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.repository.GrupoServicoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos-servico")
@CrossOrigin(origins = "*")
public class GrupoServicoController {

    private final GrupoServicoRepository repository;

    // Injeção de dependência via construtor
    public GrupoServicoController(GrupoServicoRepository repository) {
        this.repository = repository;
    }

    // Endpoint para buscar todos os registros
    @GetMapping
    public List<GrupoServico> listarTodos() {
        return repository.findAll();
    }

    // Endpoint para buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<GrupoServico> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}