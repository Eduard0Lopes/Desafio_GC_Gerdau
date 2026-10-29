package com.gerdau.desafio.config;

import com.gerdau.desafio.exception.AcessoNegadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    public static final String HEADER_USUARIO = "X-Usuario-Id";
    public static final String HEADER_PAPEL = "X-Usuario-Papel";

    public static final String PAPEL_ADMIN = "ADMIN";
    public static final String PAPEL_USUARIO = "USUARIO";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        String metodo = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        String papel = request.getHeader(HEADER_PAPEL);
        boolean rotaGovernanca = uri.contains("/api/v1/governanca");
        boolean edicaoDePedido = "PATCH".equals(metodo) && uri.contains("/api/v1/codigos-completos/");
        boolean alteracaoOuExclusao = (!edicaoDePedido
                && ("DELETE".equals(metodo) || "PUT".equals(metodo) || "PATCH".equals(metodo)))
                || uri.contains("/exclusao-em-massa") || uri.contains("/catalogo/importacao")
                || uri.contains("/catalogo/situacao") || uri.contains("/catalogo/marcar-excecao")
                || uri.contains("/catalogo/transferir-vinculos");
        if (!rotaGovernanca && !alteracaoOuExclusao) {

            return true;
        }
        if (PAPEL_ADMIN.equalsIgnoreCase(papel == null ? "" : papel.trim())) {
            return true;
        }
        throw new AcessoNegadoException(rotaGovernanca
                ? "Acesso restrito à Governança de Dados: o perfil informado não tem permissão "
                        + "(envie o header " + HEADER_PAPEL + ": ADMIN)."
                : "Acesso restrito: " + metodo.toLowerCase()
                        + " em componentes do catálogo exige o perfil ADMIN (envie o header "
                        + HEADER_PAPEL + ": ADMIN).");
    }
}
