package br.com.gerdau.servicos.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança (doc. seção 13). A autorização é SEMPRE verificada no servidor (@PreAuthorize nos controllers).
 * <ul>
 *   <li>Perfil padrão: HTTP Basic com 4 usuários de demonstração (senha vem de APP_DEV_PASSWORD).</li>
 *   <li>Perfil "oidc": valida JWT de um provedor corporativo (OAuth 2.0 / OpenID Connect).</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health/**", "/actuator/info",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    @Bean
    @Profile("!oidc")
    SecurityFilterChain basicChain(HttpSecurity http) throws Exception {
        return common(http).httpBasic(Customizer.withDefaults()).build();
    }

    @Bean
    @Profile("oidc")
    SecurityFilterChain oidcChain(HttpSecurity http, AppProperties props) throws Exception {
        return common(http)
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter(props))))
                .build();
    }

    /** API stateless, sem cookies de sessão: por isso CSRF é desativado. */
    private HttpSecurity common(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated());
    }

    private JwtAuthenticationConverter jwtConverter(AppProperties props) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(props.security().rolesClaim());
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * Custo baixo de propósito: no modo Basic a senha é verificada a cada requisição
     * (autocomplete dispara muitas). Este modo é só para desenvolvimento/demonstração.
     */
    @Bean
    @Profile("!oidc")
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    @Profile("!oidc")
    UserDetailsService devUsers(AppProperties props, PasswordEncoder encoder) {
        String password = props.security().devPassword();
        if (password == null || password.length() < 8) {
            throw new IllegalStateException(
                    "Defina APP_DEV_PASSWORD (mínimo 8 caracteres) no arquivo .env — veja docs/COMO-RODAR.md.");
        }
        String hash = encoder.encode(password);
        return new InMemoryUserDetailsManager(
                User.withUsername("consultor").password(hash)
                        .roles("CONSULTOR").build(),
                User.withUsername("solicitante").password(hash)
                        .roles("CONSULTOR", "SOLICITANTE").build(),
                User.withUsername("admin.dados").password(hash)
                        .roles("CONSULTOR", "SOLICITANTE", "ADMIN_DADOS").build(),
                User.withUsername("admin.tecnico").password(hash)
                        .roles("CONSULTOR", "SOLICITANTE", "ADMIN_DADOS", "ADMIN_TECNICO").build());
    }
}
