package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.response.MatrizDeParaResponse;
import com.gerdau.desafio.exception.ResourceNotFoundException;
import com.gerdau.desafio.model.MatrizDePara;
import com.gerdau.desafio.repository.MatrizDeParaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta da matriz de-para (seção 9.6): rastreabilidade entre os códigos
 * legados e os códigos canônicos novos.
 */
@RestController
@RequestMapping("/api/v1/matriz-de-para")
@Tag(name = "Matriz De-Para", description = "Rastreabilidade legado → código canônico (seção 9.6)")
public class MatrizDeParaController {

    private final MatrizDeParaRepository repository;

    public MatrizDeParaController(MatrizDeParaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Consultar a matriz (?query= por legado, descrição ou newId)")
    public ResponseEntity<List<MatrizDeParaResponse>> listar(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int limite) {
        List<MatrizDePara> itens = (query == null || query.isBlank())
                ? repository.findAll(PageRequest.of(0, Math.min(limite, 100))).getContent()
                : repository.buscarPorTexto("(?i)" + java.util.regex.Pattern.quote(query.trim()),
                        PageRequest.of(0, Math.min(limite, 100)));
        return ResponseEntity.ok(itens.stream().map(this::paraResponse).toList());
    }

    @GetMapping("/legado/{legacyId}")
    @Operation(summary = "De-para por código legado")
    public ResponseEntity<List<MatrizDeParaResponse>> porLegacyId(@PathVariable String legacyId) {
        return ResponseEntity.ok(repository.findByLegacyDataLegacyId(legacyId).stream()
                .map(this::paraResponse).toList());
    }

    @GetMapping("/novo/{newId}")
    @Operation(summary = "De-para por código canônico novo (todos os legados consolidados)")
    public ResponseEntity<List<MatrizDeParaResponse>> porNewId(@PathVariable String newId) {
        return ResponseEntity.ok(repository.findByNewId(newId).stream()
                .map(this::paraResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatrizDeParaResponse> porId(@PathVariable String id) {
        MatrizDePara doc = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro da matriz", id));
        return ResponseEntity.ok(paraResponse(doc));
    }

    private MatrizDeParaResponse paraResponse(MatrizDePara m) {
        MatrizDePara.LegacyData l = m.getLegacyData();
        return new MatrizDeParaResponse(m.getId(), m.getNewId(), m.getDescricaoCanonica(), m.getGrupoServico(),
                m.getUnidadeMedida(), m.getDecision(), m.getScore(), m.getReason(),
                l == null ? null : new MatrizDeParaResponse.LegacyDataResponse(l.getLegacyId(),
                        l.getLegacyDescription(), l.getLegacySource(), l.getCodigoFornecedor(),
                        l.getAlertaSap(), l.getQuantidadeCaracteres()),
                m.getStatusRevisao(), m.getReviewedBy(), m.getReviewedAt(), m.getObservacaoRevisao());
    }
}
