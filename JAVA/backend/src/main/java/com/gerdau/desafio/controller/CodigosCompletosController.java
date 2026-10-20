package com.gerdau.desafio.controller;

import com.gerdau.desafio.dto.request.CriarCodigoCompletoRequest;
import com.gerdau.desafio.dto.request.PreviewRequest;
import com.gerdau.desafio.dto.request.VinculacaoLoteRequest;
import com.gerdau.desafio.dto.response.CodigoCompletoResponse;
import com.gerdau.desafio.dto.response.LoteVinculacaoResponse;
import com.gerdau.desafio.dto.response.PreviewResponse;
import com.gerdau.desafio.service.CodigoCompletoService;
import com.gerdau.desafio.service.VinculacaoLoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/codigos-completos")
@Tag(name = "Códigos Completos", description = "Composição Grupo de Serviço + Código de Fornecedor + Código de Serviço + Unidade de Medida, com governança de entrada (seções 7.5, 10.2 e 10.3)")
public class CodigosCompletosController {

    private final CodigoCompletoService service;
    private final VinculacaoLoteService vinculacaoLoteService;

    public CodigosCompletosController(CodigoCompletoService service, VinculacaoLoteService vinculacaoLoteService) {
        this.service = service;
        this.vinculacaoLoteService = vinculacaoLoteService;
    }

    @GetMapping
    @Operation(summary = "Listar códigos completos (filtro opcional por status)")
    public ResponseEntity<List<CodigoCompletoResponse>> listar(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.listarTodos(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CodigoCompletoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping("/preview")
    @Operation(summary = "Prévia antes de salvar",
            description = "Monta a composição, verifica duplicidade em todos os componentes e recomenda "
                    + "a decisão: PERMITIR, ALERTAR ou REVISAR (seção 10.2).")
    public ResponseEntity<PreviewResponse> preview(@RequestBody PreviewRequest request) {
        return ResponseEntity.ok(service.preview(request));
    }

    @PostMapping
    @Operation(summary = "Criar código completo (com governança)",
            description = "Bloqueia duplicidades (409), aceita justificativa auditada ou envia para "
                    + "validação humana. Suporta o header Idempotency-Key (seção 10.3): uma requisição "
                    + "repetida retorna 200 com o registro existente (nada novo é criado); só uma "
                    + "criação real retorna 201.")
    public ResponseEntity<CodigoCompletoResponse> criar(
            @Valid @RequestBody CriarCodigoCompletoRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        CodigoCompletoService.ResultadoCriacao resultado = service.criar(request, idempotencyKey);
        return ResponseEntity
                .status(resultado.requisicaoRepetida() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(resultado.resposta());
    }

    @PostMapping("/lote")
    @Operation(summary = "Vincular vários Códigos de Serviço de uma vez (vinculação em massa)",
            description = "Grupo de Serviço, Código de Fornecedor e Unidade de Medida fixos para toda a "
                    + "lista. Cada item é resolvido pelas regras de negócio: código existente é "
                    + "reutilizado, código novo é criado e, havendo similaridade alertada, o pedido vai "
                    + "para a Governança. Uma linha que falha não derruba as demais.")
    public ResponseEntity<LoteVinculacaoResponse> vincularEmLote(
            @Valid @RequestBody VinculacaoLoteRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vinculacaoLoteService.vincularEmLote(request, idempotencyKey));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Editar pedido pendente (descrição e/ou justificativa)")
    public ResponseEntity<CodigoCompletoResponse> atualizarPedido(
            @PathVariable String id,
            @RequestBody com.gerdau.desafio.dto.request.AtualizarPedidoRequest request) {
        return ResponseEntity.ok(service.atualizarPedido(id, request.descricao(), request.justificativa()));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar pedido pendente (libera a composição para novo envio)")
    public ResponseEntity<CodigoCompletoResponse> cancelarPedido(@PathVariable String id) {
        return ResponseEntity.ok(service.cancelarPedido(id));
    }
}
