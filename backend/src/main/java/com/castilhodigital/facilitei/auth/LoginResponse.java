package com.castilhodigital.facilitei.auth;

public record LoginResponse(
        String token,
        Long tenantId,
        String tenantSlug,
        String email
) {
}
