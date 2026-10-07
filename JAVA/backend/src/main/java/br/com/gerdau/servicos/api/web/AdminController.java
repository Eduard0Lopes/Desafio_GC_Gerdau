package br.com.gerdau.servicos.api.web;

import br.com.gerdau.servicos.api.dto.AdminDtos.DiagnosticoResponse;
import br.com.gerdau.servicos.api.dto.AdminDtos.MeResponse;
import br.com.gerdau.servicos.api.service.DiagnosticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Sessão e diagnóstico")
public class AdminController {

    private final DiagnosticsService diagnostics;

    public AdminController(DiagnosticsService diagnostics) {
        this.diagnostics = diagnostics;
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado e perfis (usado pelo login do desktop)")
    public MeResponse me(Authentication authentication) {
        return new MeResponse(authentication.getName(), authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList());
    }

    @GetMapping("/admin/diagnostico")
    @PreAuthorize("hasRole('ADMIN_TECNICO')")
    @Operation(summary = "Banco conectado, coleções, contagens e índices de busca")
    public DiagnosticoResponse diagnostico() {
        return diagnostics.snapshot();
    }
}
