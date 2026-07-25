package com.castilhodigital.facilitei.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.cliente.ClienteService;
import com.castilhodigital.facilitei.common.exception.CodigoVerificacaoInvalidoException;
import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ClienteAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClienteAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private CodigoVerificacaoService codigoVerificacaoService;

    @MockitoBean
    private OtpRateLimiter otpRateLimiter;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void solicitarCodigoComTelefoneValidoRetorna204() throws Exception {
        when(clienteService.normalizarTelefone("(63) 99999-9999")).thenReturn("5563999999999");

        SolicitarCodigoRequest request = new SolicitarCodigoRequest("(63) 99999-9999");

        mockMvc.perform(post("/api/auth/portal/solicitar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(otpRateLimiter).verificarLimiteEnvio("5563999999999");
        verify(codigoVerificacaoService).gerarEEnviar("5563999999999");
        verify(otpRateLimiter).registrarEnvio("5563999999999");
    }

    @Test
    void solicitarCodigoComTelefoneEmBrancoRetorna400() throws Exception {
        SolicitarCodigoRequest request = new SolicitarCodigoRequest("  ");

        mockMvc.perform(post("/api/auth/portal/solicitar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitarCodigoComLimiteExcedidoRetorna429() throws Exception {
        when(clienteService.normalizarTelefone(any())).thenReturn("5563999999999");
        doThrow(new LimiteDeRequisicoesExcedidoException("Muitos codigos solicitados."))
                .when(otpRateLimiter).verificarLimiteEnvio("5563999999999");

        SolicitarCodigoRequest request = new SolicitarCodigoRequest("5563999999999");

        mockMvc.perform(post("/api/auth/portal/solicitar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void verificarCodigoCorretoRetorna200ComToken() throws Exception {
        when(clienteService.normalizarTelefone("5563999999999")).thenReturn("5563999999999");
        Cliente cliente = new Cliente();
        cliente.setTelefone("5563999999999");
        cliente.setNome("Maria");
        ReflectionTestUtils.setField(cliente, "id", 3L);
        when(clienteService.buscarOuCriarPorTelefone("5563999999999", "Maria")).thenReturn(cliente);
        when(jwtService.gerarTokenCliente(cliente)).thenReturn("um.token.jwt");

        VerificarCodigoRequest request = new VerificarCodigoRequest("5563999999999", "123456", "Maria");

        mockMvc.perform(post("/api/auth/portal/verificar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("um.token.jwt"))
                .andExpect(jsonPath("$.clienteId").value(3))
                .andExpect(jsonPath("$.nome").value("Maria"));

        verify(otpRateLimiter).registrarSucessoVerificacao("5563999999999");
    }

    @Test
    void verificarCodigoErradoRetorna401ERegistraFalha() throws Exception {
        when(clienteService.normalizarTelefone("5563999999999")).thenReturn("5563999999999");
        doThrow(new CodigoVerificacaoInvalidoException())
                .when(codigoVerificacaoService).verificar("5563999999999", "000000");

        VerificarCodigoRequest request = new VerificarCodigoRequest("5563999999999", "000000", "Maria");

        mockMvc.perform(post("/api/auth/portal/verificar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(otpRateLimiter).registrarFalhaVerificacao("5563999999999");
    }

    @Test
    void verificarCodigoComLimiteDeVerificacaoExcedidoRetorna429() throws Exception {
        when(clienteService.normalizarTelefone(any())).thenReturn("5563999999999");
        doThrow(new LimiteDeRequisicoesExcedidoException("Muitas tentativas."))
                .when(otpRateLimiter).verificarLimiteVerificacao("5563999999999");

        VerificarCodigoRequest request = new VerificarCodigoRequest("5563999999999", "123456", "Maria");

        mockMvc.perform(post("/api/auth/portal/verificar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

}
