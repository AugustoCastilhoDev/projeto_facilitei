package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Extrai dados do cliente autenticado a partir do token JWT validado pelo Spring Security. */
@Component
public class AuthenticatedClienteResolver {

    public Long clienteIdAtual() {
        Number clienteId = jwtAtual().getClaim("clienteId");
        return clienteId.longValue();
    }

    public String telefoneAtual() {
        return jwtAtual().getSubject();
    }

    private Jwt jwtAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        throw new AcessoNegadoException("Nenhum cliente autenticado.");
    }

}
