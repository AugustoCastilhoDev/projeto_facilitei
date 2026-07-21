package com.castilhodigital.facilitei.auth;

public record RegistrarTenantResponse(
        Long tenantId,
        String slug,
        Long adminUserId,
        String adminEmail
) {
}
