package br.com.gerdau.servicos.api.web;

import br.com.gerdau.servicos.api.dto.ItemDtos.CriarItemRequest;
import br.com.gerdau.servicos.api.dto.ItemDtos.ItemCriadoResponse;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilaridadeRequest;
import br.com.gerdau.servicos.api.dto.SimilarityDtos.SimilaridadeResponse;
import br.com.gerdau.servicos.api.service.ItemService;
import br.com.gerdau.servicos.api.service.SimilarityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/itens")
@Tag(name = "Itens", description = "Cadastro de novos componentes com controle de similaridade")
public class ItemController {

    private final SimilarityService similarity;
    private final ItemService items;
    private final AuditResolver audit;

    public ItemController(SimilarityService similarity, ItemService items, AuditResolver audit) {
        this.similarity = similarity;
        this.items = items;
        this.audit = audit;
    }

    @PostMapping("/similaridade")
    @PreAuthorize("hasRole('SOLICITANTE')")
    @Operation(summary = "Avalia itens semelhantes (nota 0..1, motivo e decisão PERMITIR/ALERTAR/REVISAR)")
    public SimilaridadeResponse similaridade(@Valid @RequestBody SimilaridadeRequest request) {
        return similarity.avaliar(request.tipo(), request.texto(), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SOLICITANTE')")
    @Operation(summary = "Cadastra um novo item (idempotente: exige o cabeçalho Idempotency-Key). "
            + "409 SIMILARIDADE_ALTA/SIMILARIDADE_INTERMEDIARIA quando a política exigir reutilizar/justificar.")
    public ItemCriadoResponse criar(@Valid @RequestBody CriarItemRequest request,
                                    @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                    Authentication authentication, HttpServletRequest http) {
        return items.criar(request, audit.from(authentication, http), idempotencyKey);
    }
}
