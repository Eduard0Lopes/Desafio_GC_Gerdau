package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.response.GrupoServicoResponse;
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
@RequestMapping("/api/v1/grupos-servico")
@Tag(name = "Grupos de Serviço", description = "Componente Grupo de Serviço — seção 10.1")
public class GrupoServicoController {

    public record NovoGrupoRequest(
            @NotBlank(message = "O nome é obrigatório.")
            @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
            String nome) {
    }

    private final CatalogoService service;

    public GrupoServicoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar/buscar grupos (?query= para os seletores)")
    public ResponseEntity<List<GrupoServicoResponse>> listar(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int limite,
            @RequestParam(defaultValue = "false") boolean incluirTodos) {
        return ResponseEntity.ok(service.grupos(query, Math.min(Math.max(limite, 1), 20000), incluirTodos));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo grupo (com verificação de duplicidade)")
    public ResponseEntity<GrupoServicoResponse> criar(@Valid @RequestBody NovoGrupoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarGrupo(request.nome()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir grupo (apenas ADMIN)",
            description = "Ação auditada. Bloqueada (409) enquanto o grupo estiver vinculado a códigos completos.")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        service.excluirGrupo(id);
        return ResponseEntity.noContent().build();
    }
}
