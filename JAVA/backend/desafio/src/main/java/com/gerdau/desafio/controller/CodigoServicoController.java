package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.response.CodigoServicoResponse;
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
@RequestMapping("/api/v1/codigos-servico")
@Tag(name = "Códigos de Serviço", description = "Componente Código de Serviço — limite SAP de 40 caracteres (seções 10.1 e 11.7)")
public class CodigoServicoController {

    public record NovoServicoRequest(
            @NotBlank(message = "A descrição é obrigatória.")
            @Size(max = 40, message = "A descrição não pode exceder 40 caracteres (regra SAP).")
            String descricao) {
    }

    private final CatalogoService service;

    public CodigoServicoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar/buscar serviços (?query= para os seletores)")
    public ResponseEntity<List<CodigoServicoResponse>> listar(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(service.servicos(query, Math.min(Math.max(limite, 1), 100)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoServicoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.servicoPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo serviço (com verificação de duplicidade)")
    public ResponseEntity<CodigoServicoResponse> criar(@Valid @RequestBody NovoServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarServico(request.descricao()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir serviço (apenas ADMIN)",
            description = "Ação auditada. Bloqueada (409) enquanto estiver vinculado a códigos completos.")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluirServico(id);
        return ResponseEntity.noContent().build();
    }
}
