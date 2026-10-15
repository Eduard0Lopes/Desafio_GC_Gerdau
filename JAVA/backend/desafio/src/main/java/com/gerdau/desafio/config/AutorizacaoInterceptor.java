package com.gerdau.desafio.config;

import com.gerdau.desafio.exception.AcessoNegadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Controle de permissões (documento técnico, seção 12.2) — aplicado no SERVIDOR:
 * esconder botões na interface não é controle de segurança.
 *
 * Perfis:
 *   USUARIO (padrão) — consulta, cadastro e fluxo de vinculação;
 *   ADMIN            — além disso, acessa a esteira de governança (aprovar/revisar).
 *
 * A identidade via headers ({@code X-Usuario-Id}, {@code X-Usuario-Papel}) é o
 * modelo de demonstração; em produção, o papel deve ser derivado do token
 * autenticado (OAuth 2.0/OIDC — seção 13.1).
 */
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
        boolean rotaGovernanca = uri.contains("/api/v1/governanca");
        boolean alteracaoOuExclusao = "DELETE".equals(metodo) || "PUT".equals(metodo) || "PATCH".equals(metodo);
        if (!rotaGovernanca && !alteracaoOuExclusao) {
            // consulta e cadastro seguem liberados para todos os perfis (seção 12.2)
            return true;
        }
        String papel = request.getHeader(HEADER_PAPEL);
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
