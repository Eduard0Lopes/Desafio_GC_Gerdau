package com.gerdau.desafio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra o controle de permissões (seção 12.2): governança exige ADMIN sempre;
 *  nos demais endpoints, exclusão/alteração também exige ADMIN. */
@Configuration
public class AutorizacaoConfig implements WebMvcConfigurer {

    private final AutorizacaoInterceptor autorizacaoInterceptor;

    public AutorizacaoConfig(AutorizacaoInterceptor autorizacaoInterceptor) {
        this.autorizacaoInterceptor = autorizacaoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autorizacaoInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
