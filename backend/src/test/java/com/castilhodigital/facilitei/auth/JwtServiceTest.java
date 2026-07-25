package com.castilhodigital.facilitei.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.user.Role;
import com.castilhodigital.facilitei.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Encoder e decoder reais (nao mockados) de proposito: o que mais importa
 * aqui e o round-trip completo dos claims (principalmente tenantId, que na
 * pratica pode voltar como Integer ou Long dependendo do parser JSON - ver
 * AuthenticatedTenantResolver).
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-com-pelo-menos-256-bits-para-hs256-0123456789";

    private JwtDecoder jwtDecoder;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        jwtService = new JwtService(jwtEncoder, 60);
    }

    @Test
    void gerarTokenIncluiClaimsEsperados() {
        Tenant tenant = new Tenant();
        tenant.setSlug("barbearia-do-ze");
        ReflectionTestUtils.setField(tenant, "id", 7L);

        User user = new User();
        user.setTenant(tenant);
        user.setEmail("ze@example.com");
        user.setRole(Role.ADMIN);

        String token = jwtService.gerarToken(user);
        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("ze@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");

        Number tenantId = jwt.getClaim("tenantId");
        assertThat(tenantId.longValue()).isEqualTo(7L);

        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        assertThat(jwt.getIssuedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void gerarTokenClienteIncluiClienteIdERoleClienteSemClaimDeTenant() {
        Cliente cliente = new Cliente();
        cliente.setTelefone("5563999999999");
        cliente.setNome("Maria");
        ReflectionTestUtils.setField(cliente, "id", 3L);

        String token = jwtService.gerarTokenCliente(cliente);
        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("5563999999999");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CLIENTE");

        Number clienteId = jwt.getClaim("clienteId");
        assertThat(clienteId.longValue()).isEqualTo(3L);

        assertThat(jwt.hasClaim("tenantId")).isFalse();
    }

}
