package br.com.gerdau.servicos.api.web;

import br.com.gerdau.servicos.api.domain.SolicitacaoGovernanca.Status;
import br.com.gerdau.servicos.api.dto.GovernancaDtos.DecisaoRequest;
import br.com.gerdau.servicos.api.dto.GovernancaDtos.SolicitacaoResponse;
import br.com.gerdau.servicos.api.dto.GovernancaDtos.SolicitarGovernancaRequest;
import br.com.gerdau.servicos.api.service.GovernancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/governanca")
@Validated
@Tag(name = "Governança", description = "Validação humana de cadastros muito semelhantes")
public class GovernancaController {

    private final GovernancaService service;
    private final AuditResolver audit;

    public GovernancaController(GovernancaService service, AuditResolver audit) {
        this.service = service;
        this.audit = audit;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SOLICITANTE')")
    @Operation(summary = "Envia um cadastro para a Governança (justificativa obrigatória; idempotente)")
    public SolicitacaoResponse solicitar(@Valid @RequestBody SolicitarGovernancaRequest request,
                                         @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                         Authentication authentication, HttpServletRequest http) {
        return service.solicitar(request, audit.from(authentication, http), idempotencyKey);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_DADOS')")
    @Operation(summary = "Lista solicitações por status (padrão: PENDENTE)")
    public List<SolicitacaoResponse> listar(@RequestParam(defaultValue = "PENDENTE") Status status,
                                            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return service.listar(status, limit);
    }

    @PostMapping("/{id}/decisao")
    @PreAuthorize("hasRole('ADMIN_DADOS')")
    @Operation(summary = "Aprova (cria o item) ou rejeita a solicitação. Não permite autoaprovação.")
    public SolicitacaoResponse decidir(@PathVariable @Pattern(regexp = "^[A-Za-z0-9]{1,40}$") String id,
                                       @Valid @RequestBody DecisaoRequest request,
                                       Authentication authentication, HttpServletRequest http) {
        return service.decidir(id, request, audit.from(authentication, http));
    }
}
