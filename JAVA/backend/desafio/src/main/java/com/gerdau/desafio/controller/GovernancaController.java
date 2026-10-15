package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.GovernancaDecisaoRequest;
import com.gerdau.desafio.dto.request.RevisaoMatrizRequest;
import com.gerdau.desafio.dto.response.ExcecaoResponse;
import com.gerdau.desafio.dto.response.ItemGovernancaResponse;
import com.gerdau.desafio.service.GovernancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/governanca")
@Tag(name = "Governança", description = "Revisão humana — histórico da matriz e novos cadastros (seção 12)")
public class GovernancaController {

    private final GovernancaService service;

    public GovernancaController(GovernancaService service) {
        this.service = service;
    }

    @GetMapping("/pendentes")
    @Operation(summary = "Fila de validação (histórico + novos cadastros)",
            description = "origem: HISTORICO | NOVOS | TODOS · status: PENDENTE | REVISADO | SEM_PENDENCIA · "
                    + "decision: DUPLICIDADE | ALERTA | ORIGINAL · pagina: 0, 1, 2…")
    public ResponseEntity<List<ItemGovernancaResponse>> listarPendentes(
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") int limite,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String decision,
            @RequestParam(defaultValue = "0") int pagina) {
        return ResponseEntity.ok(service.listarPendentes(origem, query,
                Math.min(Math.max(limite, 1), 500), status, decision, pagina));
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas detalhadas do Painel Admin (gráficos e exportação)")
    public ResponseEntity<Map<String, Object>> estatisticas() {
        return ResponseEntity.ok(service.estatisticas());
    }

    @GetMapping("/fechamentos")
    @Operation(summary = "Fechamentos (decisões humanas) por período — métrica mensal",
            description = "Parâmetros opcionais de data (AAAA-MM-DD): de, ate. Sem parâmetros, todo o histórico.")
    public ResponseEntity<Map<String, Object>> fechamentos(
            @RequestParam(required = false) String de,
            @RequestParam(required = false) String ate) {
        return ResponseEntity.ok(service.fechamentos(de, ate));
    }

    @GetMapping("/fila/contagem")
    @Operation(summary = "Total de itens da fila (para paginação: Página X de Y)")
    public ResponseEntity<Map<String, Object>> contarFila(
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String query) {
        long total = service.contarFila(origem, status, decision, query);
        return ResponseEntity.ok(Map.of("total", total));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Indicadores da governança (impacto e pendências)")
    public ResponseEntity<Map<String, Object>> resumo() {
        return ResponseEntity.ok(service.resumo());
    }

    @PostMapping("/historico/{id}/revisar")
    @Operation(summary = "Revisar registro histórico (reviewedBy/reviewedAt — seção 9.6)")
    public ResponseEntity<ItemGovernancaResponse> revisarHistorico(
            @PathVariable String id, @Valid @RequestBody RevisaoMatrizRequest request) {
        return ResponseEntity.ok(service.revisarHistorico(id, request));
    }

    @PostMapping("/historico/{id}/reverter")
    @Operation(summary = "Reverter decisão humana (o item volta para a fila de pendentes)")
    public ResponseEntity<ItemGovernancaResponse> reverterRevisao(
            @PathVariable String id, @Valid @RequestBody RevisaoMatrizRequest request) {
        return ResponseEntity.ok(service.reverterRevisao(id, request));
    }

    @PostMapping("/historico/consolidar-em-massa")
    @Operation(summary = "Consolidar em massa os pendentes de uma decisão (evita revisão um a um)",
            description = "body: { revisadoPor, acao?, observacao? } · query decision: DUPLICIDADE | ALERTA")
    public ResponseEntity<Map<String, Object>> consolidarEmMassa(
            @RequestParam(defaultValue = "DUPLICIDADE") String decision,
            @Valid @RequestBody RevisaoMatrizRequest request) {
        return ResponseEntity.ok(service.consolidarEmMassa(decision, request));
    }

    @PostMapping("/pendentes/{id}/aprovar")
    public ResponseEntity<ItemGovernancaResponse> aprovarNovo(
            @PathVariable String id, @Valid @RequestBody GovernancaDecisaoRequest request) {
        return ResponseEntity.ok(service.aprovarNovo(id, request));
    }

    @PostMapping("/pendentes/{id}/rejeitar")
    public ResponseEntity<ItemGovernancaResponse> rejeitarNovo(
            @PathVariable String id, @Valid @RequestBody GovernancaDecisaoRequest request) {
        return ResponseEntity.ok(service.rejeitarNovo(id, request));
    }

    @GetMapping("/excecoes")
    @Operation(summary = "Registro de exceções/auditoria (últimas 50 — seção 12.3)")
    public ResponseEntity<List<ExcecaoResponse>> listarExcecoes() {
        return ResponseEntity.ok(service.listarExcecoes());
    }

    @GetMapping("/eventos")
    @Operation(summary = "Trilha de auditoria completa (seções 5 e 9)",
            description = "Toda criação, decisão, bloqueio e exceção com ator, timestamp do servidor (UTC), "
                    + "candidatos, scores e correlationId. Filtro opcional por ação: "
                    + "CRIACAO_COMPONENTE | CRIACAO_CODIGO_COMPLETO | BLOQUEIO | EXCECAO | APROVACAO | REJEICAO | REVISAO_HISTORICA")
    public ResponseEntity<List<Map<String, Object>>> listarEventos(
            @RequestParam(required = false) String acao) {
        return ResponseEntity.ok(service.listarEventos(acao));
    }
}
