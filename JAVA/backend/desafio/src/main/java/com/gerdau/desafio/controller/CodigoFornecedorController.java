package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.CodigoFornecedor;
import com.gerdau.desafio.repository.CodigoFornecedorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigos-fornecedor")
@CrossOrigin(origins = "*")
public class CodigoFornecedorController {

    private final CodigoFornecedorRepository repository;

    public CodigoFornecedorController(CodigoFornecedorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CodigoFornecedor> listarTodos() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoFornecedor> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CodigoFornecedor> salvar(@RequestBody CodigoFornecedor codigoFornecedor) {
        if (codigoFornecedor.getNomeNormalizado() == null || codigoFornecedor.getNomeNormalizado().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do grupo de serviço é obrigatório.");
        }

        CodigoFornecedor salvo = repository.save(codigoFornecedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}