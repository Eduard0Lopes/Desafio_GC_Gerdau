package br.com.gerdau.servicos.api.web;

import br.com.gerdau.servicos.api.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuditResolver {

    private final boolean recordIp;

    public AuditResolver(AppProperties props) {
        this.recordIp = props.audit().recordIp();
    }

    public AuditInfo from(Authentication authentication, HttpServletRequest request) {
        return new AuditInfo(authentication.getName(), recordIp ? request.getRemoteAddr() : null);
    }
}
