package com.castilhodigital.facilitei.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * O portal do cliente introduz um novo valor de claim "role" (CLIENTE, ao
 * lado do ja existente ADMIN) sem qualquer mudanca em JwtConfig - o
 * conversor ja e generico (qualquer string em "role" vira ROLE_<valor>).
 * Este teste existe para confirmar isso por escrito, nao para testar
 * comportamento novo.
 */
class JwtConfigTest {

    private final JwtAuthenticationConverter converter = new JwtConfig().jwtAuthenticationConverter();

    @Test
    void roleClienteViraGrantedAuthorityRoleCliente() {
        Jwt jwt = jwtComRole("CLIENTE");

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // "FACTOR_BEARER" tambem aparece aqui (autoridade padrao que o Spring
        // Security adiciona para tokens Bearer, independente do nosso
        // conversor customizado) - o que importa e que ROLE_CLIENTE esta presente.
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).contains("ROLE_CLIENTE");
    }

    @Test
    void roleAdminViraGrantedAuthorityRoleAdmin() {
        Jwt jwt = jwtComRole("ADMIN");

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN");
    }

    private Jwt jwtComRole(String role) {
        Instant agora = Instant.now();
        return new Jwt("token-fake", agora, agora.plusSeconds(60),
                Map.of("alg", "HS256"), Map.of("sub", "assunto", "role", role));
    }

}
