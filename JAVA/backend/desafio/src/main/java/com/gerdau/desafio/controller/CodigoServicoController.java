package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.CodigoServicoRequest;
import com.gerdau.desafio.dto.response.CodigoServicoResponse;
import com.gerdau.desafio.dto.response.PageResponse;
import com.gerdau.desafio.dto.response.SimilaridadeResponse;
import com.gerdau.desafio.service.CodigoServicoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigos-servico")
public class CodigoServicoController {

    private final CodigoServicoService service;

    public CodigoServicoController(CodigoServicoService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CodigoServicoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return service.listar(q, page, size);
    }

    @GetMapping("/similares")
    public List<SimilaridadeResponse> similares(
            @RequestParam @NotBlank String descricao,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limite,
            @RequestParam(required = false) String ignorarId) {
        return service.buscarSimilares(descricao, limite, ignorarId);
    }

    @GetMapping("/{id}")
    public CodigoServicoResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodigoServicoResponse criar(@Valid @RequestBody CodigoServicoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public CodigoServicoResponse atualizar(@PathVariable String id, @Valid @RequestBody CodigoServicoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
