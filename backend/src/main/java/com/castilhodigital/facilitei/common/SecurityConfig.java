package com.castilhodigital.facilitei.common;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras reais de autenticacao/autorizacao (etapa 5), substituindo a
 * configuracao temporaria "permitAll" da etapa 4.
 *
 * - /api/auth/** e /api/public/** continuam abertos (cadastro/login sao
 *   pre-autenticacao; a API publica de agendamento nao tem usuario logado).
 * - /api/webhooks/** tambem fica fora do JWT: quem chama e o Asaas, nao um
 *   usuario do sistema. A autenticidade e validada dentro do proprio
 *   controller via o header "asaas-access-token" (etapa 6).
 * - /api/admin/** exige um JWT valido com role ADMIN.
 * - Sessao stateless: cada request carrega seu proprio JWT, sem estado de
 *   sessao no servidor (pre-requisito para escalar horizontalmente sem
 *   sticky sessions).
 * - CSRF desabilitado desde a etapa 4: so faz sentido para autenticacao
 *   baseada em cookie, e esta API nunca usa cookie de sessao.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/public/**", "/api/webhooks/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        // o resource server tem seu proprio entry point para token
                        // presente-mas-invalido (ex.: malformado); sem sobrescreve-lo
                        // aqui, esse caso ignora o exceptionHandling() global abaixo
                        // e responde com corpo vazio em vez de ProblemDetail.
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

}
