package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.CodigoFornecedorRequest;
import com.gerdau.desafio.dto.response.CodigoFornecedorResponse;
import com.gerdau.desafio.service.CodigoFornecedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codigos-fornecedor")
public class CodigoFornecedorController {

    private final CodigoFornecedorService service;

    public CodigoFornecedorController(CodigoFornecedorService service) {
        this.service = service;
    }

    @GetMapping
    public List<CodigoFornecedorResponse> listar(@RequestParam(required = false) String q) {
        return service.listar(q);
    }

    @GetMapping("/{id}")
    public CodigoFornecedorResponse buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodigoFornecedorResponse criar(@Valid @RequestBody CodigoFornecedorRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public CodigoFornecedorResponse atualizar(@PathVariable String id,
                                              @Valid @RequestBody CodigoFornecedorRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String id) {
        service.excluir(id);
    }
}
