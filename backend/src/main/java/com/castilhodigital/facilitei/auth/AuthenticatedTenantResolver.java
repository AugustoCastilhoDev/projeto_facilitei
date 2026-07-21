package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Extrai dados do usuario autenticado a partir do token JWT validado pelo Spring Security. */
@Component
public class AuthenticatedTenantResolver {

    public Long tenantIdAtual() {
        // getClaim retorna um numero (Long ou Integer, dependendo de como o
        // parser JSON decodifica) - Number cobre os dois casos sem risco de
        // ClassCastException.
        Number tenantId = jwtAtual().getClaim("tenantId");
        return tenantId.longValue();
    }

    public String emailAtual() {
        return jwtAtual().getSubject();
    }

    private Jwt jwtAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        throw new AcessoNegadoException("Nenhum usuario autenticado.");
    }

}
