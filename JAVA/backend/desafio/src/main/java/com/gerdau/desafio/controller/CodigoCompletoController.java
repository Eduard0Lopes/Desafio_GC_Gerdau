package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.CodigoCompletoRequest;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.PageResponse;
import com.gerdau.desafio.service.CodigoCompletoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigos-completos")
public class CodigoCompletoController {

    private final CodigoCompletoService service;

    public CodigoCompletoController(CodigoCompletoService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CodigoCompletoResponse> listar(
            @RequestParam(required = false) String gsId,
            @RequestParam(required = false) String cfId,
            @RequestParam(required = false) String csId,
            @RequestParam(required = false) String umId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return service.listar(gsId, cfId, csId, umId, q, page, size);
    }

    @GetMapping("/legado/{legacyId}")
    public List<CodigoCompletoResponse> buscarPorCodigoLegado(@PathVariable String legacyId) {
        return service.buscarPorCodigoLegado(legacyId);
    }

    @GetMapping("/{id}")
    public CodigoCompletoResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodigoCompletoResponse criar(@Valid @RequestBody CodigoCompletoRequest request) {
        return service.criar(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
