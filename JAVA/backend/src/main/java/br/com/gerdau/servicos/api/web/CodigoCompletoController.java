package br.com.gerdau.servicos.api.web;

import br.com.gerdau.servicos.api.dto.CodigoCompletoDtos.CodigoCompletoResponse;
import br.com.gerdau.servicos.api.dto.CodigoCompletoDtos.ComposicaoRequest;
import br.com.gerdau.servicos.api.dto.CodigoCompletoDtos.PreviewResponse;
import br.com.gerdau.servicos.api.service.CodigoCompletoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/codigos-completos")
@Validated
@Tag(name = "Códigos completos", description = "Composição G.S. + C.F. + C.S. + U.M.")
public class CodigoCompletoController {

    private final CodigoCompletoService service;
    private final AuditResolver audit;

    public CodigoCompletoController(CodigoCompletoService service, AuditResolver audit) {
        this.service = service;
        this.audit = audit;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('CONSULTOR')")
    @Operation(summary = "Prévia antes de salvar (ID, descrição, caracteres, duplicidade e similares)")
    public PreviewResponse preview(@Valid @RequestBody ComposicaoRequest request) {
        return service.preview(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SOLICITANTE')")
    @Operation(summary = "Cria o código completo (idempotente: exige o cabeçalho Idempotency-Key)")
    public CodigoCompletoResponse criar(@Valid @RequestBody ComposicaoRequest request,
                                        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                        Authentication authentication, HttpServletRequest http) {
        return service.criar(request, audit.from(authentication, http), idempotencyKey);
    }

    @GetMapping("/{idCompleto}")
    @PreAuthorize("hasRole('CONSULTOR')")
    @Operation(summary = "Consulta um código completo pelo ID")
    public CodigoCompletoResponse buscar(@PathVariable @Pattern(regexp = "^[A-Za-z0-9]{1,40}$") String idCompleto) {
        return service.buscarPorId(idCompleto);
    }
}
