package com.castilhodigital.facilitei.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.common.exception.CredenciaisInvalidasException;
import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Filtros de seguranca desligados de proposito: aqui testamos o contrato do
 * controller (validacao, mapeamento de excecao -> status HTTP), nao o
 * SecurityFilterChain em si - isso e coberto pelo SecurityIntegrationTest.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @Test
    void registrarComDadosValidosRetorna201() throws Exception {
        RegistrarTenantRequest request = new RegistrarTenantRequest(
                "Barbearia do Ze", "barbearia-do-ze",
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(18, 0),
                "ze@example.com", "senha12345");

        when(registrationService.registrarTenantEAdmin(any()))
                .thenReturn(new RegistrarTenantResponse(1L, "barbearia-do-ze", 10L, "ze@example.com"));

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(1))
                .andExpect(jsonPath("$.slug").value("barbearia-do-ze"))
                .andExpect(jsonPath("$.adminEmail").value("ze@example.com"));
    }

    @Test
    void registrarComEmailInvalidoRetorna400() throws Exception {
        RegistrarTenantRequest request = new RegistrarTenantRequest(
                "Barbearia do Ze", "barbearia-do-ze",
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(18, 0),
                "email-invalido", "senha12345");

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.emailAdmin").exists());
    }

    @Test
    void loginComCredenciaisValidasRetorna200ComToken() throws Exception {
        LoginRequest request = new LoginRequest("ze@example.com", "senha12345");
        when(loginService.autenticar("ze@example.com", "senha12345"))
                .thenReturn(new LoginResponse("um.token.jwt", 1L, "barbearia-do-ze", "ze@example.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("um.token.jwt"))
                .andExpect(jsonPath("$.tenantId").value(1));
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        LoginRequest request = new LoginRequest("ze@example.com", "senhaErrada");
        when(loginService.autenticar("ze@example.com", "senhaErrada"))
                .thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginAposLimiteDeTentativasExcedidoRetorna429() throws Exception {
        LoginRequest request = new LoginRequest("ze@example.com", "senhaErrada");
        org.mockito.Mockito.doThrow(new LimiteDeRequisicoesExcedidoException("Muitas tentativas de login."))
                .when(loginRateLimiter).verificarLimite(any());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

}
