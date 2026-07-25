package com.castilhodigital.facilitei.auth;

public record ClienteLoginResponse(
        String token,
        Long clienteId,
        String nome,
        String telefone
) {
}
