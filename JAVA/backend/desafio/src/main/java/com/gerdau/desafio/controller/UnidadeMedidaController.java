package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.response.UnidadeMedidaResponse;
import com.gerdau.desafio.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/unidades-medida")
@Tag(name = "Unidades de Medida", description = "Componente Unidade de Medida — seção 10.1")
public class UnidadeMedidaController {

    public record NovaUnidadeRequest(
            @NotBlank(message = "A sigla é obrigatória.")
            @Size(max = 10, message = "A sigla deve ter no máximo 10 caracteres.")
            String sigla,
            String nome) {
    }

    private final CatalogoService service;

    public UnidadeMedidaController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar/buscar unidades (?query= para os seletores)")
    public ResponseEntity<List<UnidadeMedidaResponse>> listar(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(service.unidades(query, Math.min(Math.max(limite, 1), 100)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova unidade (com verificação de duplicidade)")
    public ResponseEntity<UnidadeMedidaResponse> criar(@Valid @RequestBody NovaUnidadeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.criarUnidade(request.sigla(), request.nome()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir unidade (apenas ADMIN)",
            description = "Ação auditada. Bloqueada (409) enquanto estiver vinculada a códigos completos.")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluirUnidade(id);
        return ResponseEntity.noContent().build();
    }
}
