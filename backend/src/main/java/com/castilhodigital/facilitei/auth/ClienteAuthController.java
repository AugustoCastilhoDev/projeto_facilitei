package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.cliente.ClienteService;
import com.castilhodigital.facilitei.common.exception.CodigoVerificacaoInvalidoException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login/cadastro do cliente final do portal, sempre via codigo de
 * verificacao (OTP) por WhatsApp - sem senha. Endpoints abertos
 * (sob /api/auth/**, ja permitAll em SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth/portal")
@RequiredArgsConstructor
public class ClienteAuthController {

    private final ClienteService clienteService;
    private final CodigoVerificacaoService codigoVerificacaoService;
    private final OtpRateLimiter otpRateLimiter;
    private final JwtService jwtService;

    @PostMapping("/solicitar-codigo")
    public ResponseEntity<Void> solicitarCodigo(@Valid @RequestBody SolicitarCodigoRequest request) {
        String telefone = clienteService.normalizarTelefone(request.telefone());
        otpRateLimiter.verificarLimiteEnvio(telefone);

        codigoVerificacaoService.gerarEEnviar(telefone);
        otpRateLimiter.registrarEnvio(telefone);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verificar-codigo")
    public ClienteLoginResponse verificarCodigo(@Valid @RequestBody VerificarCodigoRequest request) {
        String telefone = clienteService.normalizarTelefone(request.telefone());
        otpRateLimiter.verificarLimiteVerificacao(telefone);

        try {
            codigoVerificacaoService.verificar(telefone, request.codigo());
            otpRateLimiter.registrarSucessoVerificacao(telefone);
        } catch (CodigoVerificacaoInvalidoException ex) {
            otpRateLimiter.registrarFalhaVerificacao(telefone);
            throw ex;
        }

        Cliente cliente = clienteService.buscarOuCriarPorTelefone(telefone, request.nome());
        String token = jwtService.gerarTokenCliente(cliente);
        return new ClienteLoginResponse(token, cliente.getId(), cliente.getNome(), cliente.getTelefone());
    }

}
