package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.GrupoServicoRequest;
import com.gerdau.desafio.dto.response.GrupoServicoResponse;
import com.gerdau.desafio.service.GrupoServicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos-servico")
public class GrupoServicoController {

    private final GrupoServicoService service;

    public GrupoServicoController(GrupoServicoService service) {
        this.service = service;
    }

    @GetMapping
    public List<GrupoServicoResponse> listar(@RequestParam(required = false) String q) {
        return service.listar(q);
    }

    @GetMapping("/{id}")
    public GrupoServicoResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoServicoResponse criar(@Valid @RequestBody GrupoServicoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public GrupoServicoResponse atualizar(@PathVariable String id, @Valid @RequestBody GrupoServicoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
