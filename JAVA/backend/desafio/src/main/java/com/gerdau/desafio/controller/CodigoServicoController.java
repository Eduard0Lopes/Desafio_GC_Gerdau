package com.gerdau.desafio.controller;

import com.gerdau.desafio.model.CodigoServico;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigo-servico")
@CrossOrigin(origins = "*")
public class CodigoServicoController {

    private final CodigoServicoRepository repository;

    public CodigoServicoController(CodigoServicoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CodigoServico> listarTodos() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoServico> buscarPorId(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CodigoServico> salvar(@RequestBody CodigoServico codigoServico) {
        if (codigoServico.getDescricao() != null && codigoServico.getDescricao().length() > 40) {
            throw new IllegalArgumentException("A descrição do serviço excede o limite de 40 caracteres do SAP.");
        }

        if (codigoServico.getDescricao() != null) {
            codigoServico.setQuantidadeCaracteres(codigoServico.getDescricao().length());
        }

        if (codigoServico.getOrigem() == null || codigoServico.getOrigem().isBlank()) {
            codigoServico.setOrigem("SISTEMA");
        }

        // Regra de geração sequencial do Código SAP original
        if (codigoServico.getCodigoOriginal() == null || codigoServico.getCodigoOriginal().isBlank()) {
            long total = repository.count();
            String proximoCodigo = String.format("%06d", total + 1);
            codigoServico.setCodigoOriginal(proximoCodigo);
        }

        CodigoServico salvo = repository.save(codigoServico);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}