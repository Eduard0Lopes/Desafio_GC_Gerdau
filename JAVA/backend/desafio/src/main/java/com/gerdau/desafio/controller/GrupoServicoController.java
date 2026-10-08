package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.repository.GrupoServicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos-servico")
@CrossOrigin(origins = "*")
public class GrupoServicoController {

    private final GrupoServicoRepository repository;

    public GrupoServicoController(GrupoServicoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<GrupoServico> listarTodos() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoServico> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GrupoServico> salvar(@RequestBody GrupoServico grupoServico) {
        if (grupoServico.getNome() == null || grupoServico.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do grupo de serviço é obrigatório.");
        }

        GrupoServico salvo = repository.save(grupoServico);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}