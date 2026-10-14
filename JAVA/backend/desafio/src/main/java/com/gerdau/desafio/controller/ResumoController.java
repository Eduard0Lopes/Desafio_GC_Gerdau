package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.response.ResumoResponse;
import com.gerdau.desafio.service.ResumoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumo")
public class ResumoController {

    private final ResumoService service;

    public ResumoController(ResumoService service) {
        this.service = service;
    }

    @GetMapping
    public ResumoResponse obter() {
        return service.obter();
    }
}
