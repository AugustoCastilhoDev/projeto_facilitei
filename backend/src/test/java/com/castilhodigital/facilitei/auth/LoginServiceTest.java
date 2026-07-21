package com.castilhodigital.facilitei.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.common.exception.CredenciaisInvalidasException;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.user.User;
import com.castilhodigital.facilitei.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(userService, passwordEncoder, jwtService);
    }

    private User usuario() {
        Tenant tenant = new Tenant();
        tenant.setSlug("barbearia-do-ze");
        ReflectionTestUtils.setField(tenant, "id", 1L);

        User user = new User();
        user.setTenant(tenant);
        user.setEmail("ze@example.com");
        user.setSenhaHash("hash-bcrypt-fake");
        return user;
    }

    @Test
    void autenticarComCredenciaisCorretasRetornaToken() {
        when(userService.buscarPorEmail("ze@example.com")).thenReturn(usuario());
        when(passwordEncoder.matches("senha12345", "hash-bcrypt-fake")).thenReturn(true);
        when(jwtService.gerarToken(any())).thenReturn("um.token.jwt");

        LoginResponse response = loginService.autenticar("ze@example.com", "senha12345");

        assertThat(response.token()).isEqualTo("um.token.jwt");
        assertThat(response.tenantId()).isEqualTo(1L);
        assertThat(response.tenantSlug()).isEqualTo("barbearia-do-ze");
    }

    @Test
    void autenticarComSenhaErradaLancaCredenciaisInvalidas() {
        when(userService.buscarPorEmail("ze@example.com")).thenReturn(usuario());
        when(passwordEncoder.matches("senhaErrada", "hash-bcrypt-fake")).thenReturn(false);

        assertThatThrownBy(() -> loginService.autenticar("ze@example.com", "senhaErrada"))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void autenticarComEmailInexistenteLancaCredenciaisInvalidas() {
        when(userService.buscarPorEmail("naoexiste@example.com"))
                .thenThrow(new EntidadeNaoEncontradaException("Usuario nao encontrado."));

        assertThatThrownBy(() -> loginService.autenticar("naoexiste@example.com", "qualquer"))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

}
