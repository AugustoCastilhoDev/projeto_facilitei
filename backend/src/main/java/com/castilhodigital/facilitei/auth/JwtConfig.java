package com.castilhodigital.facilitei.auth;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Usa o suporte nativo do Spring Security a JWT (Resource Server + Nimbus)
 * em vez de uma lib de terceiros como jjwt - decisao tomada na etapa 1 para
 * evitar risco de incompatibilidade com Jackson 3/Spring Framework 7.
 *
 * Assinatura simetrica (HS256) com o segredo de facilitei.jwt.secret: mais
 * simples que um par de chaves RSA para o MVP, adequado porque quem emite e
 * quem valida o token e o mesmo processo (nao ha terceiros validando o JWT).
 */
@Configuration
public class JwtConfig {

    @Value("${facilitei.jwt.secret}")
    private String secret;

    private SecretKeySpec chaveSimetrica() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(chaveSimetrica()).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chaveSimetrica()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /**
     * O claim "role" e uma string simples (ex.: "ADMIN"), nao uma lista -
     * por isso um conversor customizado em vez do JwtGrantedAuthoritiesConverter
     * padrao (que espera uma claim com colecao de authorities).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) {
                return List.of();
            }
            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            return List.of(authority);
        });
        return converter;
    }

}
