package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.UnidadeMedidaRequest;
import com.gerdau.desafio.dto.response.UnidadeMedidaResponse;
import com.gerdau.desafio.service.UnidadeMedidaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades-medida")
public class UnidadeMedidaController {

    private final UnidadeMedidaService service;

    public UnidadeMedidaController(UnidadeMedidaService service) {
        this.service = service;
    }

    @GetMapping
    public List<UnidadeMedidaResponse> listar(@RequestParam(required = false) String q) {
        return service.listar(q);
    }

    @GetMapping("/{id}")
    public UnidadeMedidaResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnidadeMedidaResponse criar(@Valid @RequestBody UnidadeMedidaRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public UnidadeMedidaResponse atualizar(@PathVariable String id, @Valid @RequestBody UnidadeMedidaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
