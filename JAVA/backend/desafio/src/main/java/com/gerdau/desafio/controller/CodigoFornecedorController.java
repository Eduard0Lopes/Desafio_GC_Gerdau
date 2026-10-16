package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.response.CodigoFornecedorResponse;
import com.gerdau.desafio.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/codigos-fornecedor")
@Tag(name = "Códigos de Fornecedor", description = "Componente Código de Fornecedor — seção 10.1")
public class CodigoFornecedorController {

    public record NovoFornecedorRequest(
            @NotBlank(message = "O nome é obrigatório.")
            String nome,
            String codigoSap) {
    }

    private final CatalogoService service;

    public CodigoFornecedorController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar/buscar fornecedores (?query= para os seletores)")
    public ResponseEntity<List<CodigoFornecedorResponse>> listar(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(service.fornecedores(query, Math.min(Math.max(limite, 1), 20000)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo fornecedor (com verificação de duplicidade)")
    public ResponseEntity<CodigoFornecedorResponse> criar(@Valid @RequestBody NovoFornecedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.criarFornecedor(request.nome(), request.codigoSap()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir fornecedor (apenas ADMIN)",
            description = "Ação auditada. Bloqueada (409) enquanto estiver vinculado a códigos completos.")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluirFornecedor(id);
        return ResponseEntity.noContent().build();
    }
}
