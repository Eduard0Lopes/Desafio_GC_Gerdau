package com.gerdau.desafio.controller;

import com.gerdau.desafio.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catalogo", description = "Operacoes em massa sobre os componentes do catalogo")
public class CatalogoController {

    public record ExclusaoEmMassaRequest(
            @NotBlank(message = "O tipo de componente e obrigatorio (GS, CF, CS ou UM).")
            String tipo,
            List<String> ids) {
    }

    public record SituacaoEmMassaRequest(
            @NotBlank(message = "O tipo de componente e obrigatorio (GS, CF, CS ou UM).")
            String tipo,
            List<String> ids,
            @NotBlank(message = "Informe a situacao: DESATIVADO ou ATIVO.")
            String situacao) {
    }

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @PostMapping("/exclusao-em-massa")
    @Operation(summary = "Excluir varios componentes de uma vez (apenas ADMIN)",
            description = "Cada item passa pelas mesmas regras da exclusao unitaria: vinculo bloqueia a "
                    + "exclusao e o motivo volta no relatorio. Acao auditada item a item.")
    public ResponseEntity<List<Map<String, Object>>> excluirEmMassa(@RequestBody ExclusaoEmMassaRequest request) {
        return ResponseEntity.ok(service.excluirEmMassa(request.tipo(), request.ids()));
    }

    @PostMapping("/marcar-excecao")
    @Operation(summary = "Marcar itens como caso especial (badge (!) com motivo) — apenas ADMIN",
            description = "Usado para sinalizar exceções aprovadas e duplicidades tratadas: o badge (!) "
                    + "passa a acompanhar o item em toda a tela, com o motivo no hover.")
    public ResponseEntity<Map<String, Object>> marcarExcecao(
            @RequestBody com.gerdau.desafio.dto.request.MarcarExcecaoRequest request) {
        if (Boolean.TRUE.equals(request.limpar())) {
            return ResponseEntity.ok(service.limparCasosIsolados(request.ids()));
        }
        return ResponseEntity.ok(service.marcarCasosIsolados(request.ids(), request.motivo(), request.motivos()));
    }

    @PostMapping("/situacao")
    @Operation(summary = "Desativar ou reativar componentes (apenas ADMIN)",
            description = "Soft delete: o item desativado sai das listas de uso e da similaridade, "
                    + "mas nada e perdido — vinculos, historico e auditoria continuam. Reativar "
                    + "devolve o item a circulacao. Devolve um relatorio item a item.")
    public ResponseEntity<List<Map<String, Object>>> definirSituacao(@RequestBody SituacaoEmMassaRequest request) {
        return ResponseEntity.ok(service.definirSituacaoEmMassa(request.tipo(), request.ids(), request.situacao()));
    }

    @PostMapping("/importacao")
    @Operation(summary = "Importar componentes para o catalogo (apenas ADMIN)",
            description = "Alimenta o catalogo escolhido (GS, CF, CS ou UM) SEM apagar nada: o que ja "
                    + "existe e ignorado (item mantido) e so o que e novo e adicionado. Devolve um "
                    + "relatorio por linha (adicionados x ignorados, com o motivo).")
    public ResponseEntity<Map<String, Object>> importar(
            @Valid @RequestBody com.gerdau.desafio.dto.request.ImportacaoCatalogoRequest request) {
        return ResponseEntity.ok(service.importarEmMassa(request.tipo(), request.itens()));
    }

    @GetMapping("/vinculos")
    @Operation(summary = "Quantos codigos completos referenciam o componente")
    public ResponseEntity<Map<String, Object>> vinculos(@RequestParam String tipo, @RequestParam String id) {
        return ResponseEntity.ok(Map.of("total", service.contarVinculos(tipo, id)));
    }

    @PostMapping("/transferir-vinculos")
    @Operation(summary = "Transferir vinculos para outro componente e excluir o de origem (apenas ADMIN)",
            description = "Caminho para deduplicar itens vinculados: escolha para onde vao os vinculos. "
                    + "Composicoes cujo destino ja existe sao relatadas em conflitos e nao sao alteradas.")
    public ResponseEntity<Map<String, Object>> transferirVinculos(
            @RequestBody com.gerdau.desafio.dto.request.TransferirVinculosRequest request) {
        return ResponseEntity.ok(service.transferirVinculos(request.tipo(), request.de(), request.para(),
                Boolean.TRUE.equals(request.excluirOrigem())));
    }
}
