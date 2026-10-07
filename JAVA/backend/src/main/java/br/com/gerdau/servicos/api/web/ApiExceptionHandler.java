package br.com.gerdau.servicos.api.web;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Todas as respostas de erro seguem RFC 9457 (ProblemDetail) com um campo "codigo" estável. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setTitle(ex.codigo());
        pd.setProperty("codigo", ex.codigo());
        ex.extras().forEach(pd::setProperty);
        return ResponseEntity.status(ex.status()).body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> erros.put(v.getPropertyPath().toString(), v.getMessage()));
        return problem(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Parâmetros inválidos.", erros);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Seu perfil não permite esta operação.", null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "CONFLITO_DE_VERSAO",
                "O registro foi alterado por outra pessoa. Recarregue e tente novamente.", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        // Detalhes só no log; o cliente recebe uma mensagem genérica (sem vazar internals).
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Erro interno. Informe o X-Request-Id da resposta ao suporte.", null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> erros.put(f.getField(), f.getDefaultMessage()));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requisição inválida.");
        pd.setTitle("VALIDACAO");
        pd.setProperty("codigo", "VALIDACAO");
        pd.setProperty("erros", erros);
        return ResponseEntity.badRequest().body(pd);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String codigo, String detail, Object erros) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(codigo);
        pd.setProperty("codigo", codigo);
        if (erros != null) pd.setProperty("erros", erros);
        return ResponseEntity.status(status).body(pd);
    }
}
