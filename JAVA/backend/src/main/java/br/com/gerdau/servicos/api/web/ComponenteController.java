package br.com.gerdau.servicos.api.web;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.ComponenteDtos.ComponenteDto;
import br.com.gerdau.servicos.api.service.ComponenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pesquisa dos quatro componentes por ID, nome ou descrição (autocomplete, fuzzy, ranking). */
@RestController
@RequestMapping("/api/v1")
@Validated
@PreAuthorize("hasRole('CONSULTOR')")
@Tag(name = "Componentes", description = "Busca de G.S., C.F., C.S. e U.M.")
public class ComponenteController {

    private final ComponenteService service;

    public ComponenteController(ComponenteService service) {
        this.service = service;
    }

    @GetMapping("/grupos-servico")
    @Operation(summary = "Busca Grupos de Serviço (G.S.)")
    public List<ComponenteDto> gruposServico(@RequestParam(required = false) @Size(max = 100) String query,
                                             @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return service.buscar(TipoComponente.GS, query, limit);
    }

    @GetMapping("/codigos-fornecedor")
    @Operation(summary = "Busca Códigos de Fornecedor (C.F.)")
    public List<ComponenteDto> codigosFornecedor(@RequestParam(required = false) @Size(max = 100) String query,
                                                 @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return service.buscar(TipoComponente.CF, query, limit);
    }

    @GetMapping("/codigos-servico")
    @Operation(summary = "Busca Códigos de Serviço (C.S.)")
    public List<ComponenteDto> codigosServico(@RequestParam(required = false) @Size(max = 100) String query,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return service.buscar(TipoComponente.CS, query, limit);
    }

    @GetMapping("/unidades-medida")
    @Operation(summary = "Busca Unidades de Medida (U.M.)")
    public List<ComponenteDto> unidadesMedida(@RequestParam(required = false) @Size(max = 100) String query,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return service.buscar(TipoComponente.UM, query, limit);
    }
}
