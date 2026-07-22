package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.common.exception.CredenciaisInvalidasException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/registrar")
    public ResponseEntity<RegistrarTenantResponse> registrar(@Valid @RequestBody RegistrarTenantRequest request) {
        RegistrarTenantResponse response = registrationService.registrarTenantEAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String chaveRateLimit = servletRequest.getRemoteAddr();
        loginRateLimiter.verificarLimite(chaveRateLimit);

        try {
            LoginResponse response = loginService.autenticar(request.email(), request.senha());
            loginRateLimiter.registrarSucesso(chaveRateLimit);
            return response;
        } catch (CredenciaisInvalidasException ex) {
            loginRateLimiter.registrarFalha(chaveRateLimit);
            throw ex;
        }
    }

}
