package com.castilhodigital.facilitei.tenant;

public record PublicTenantResponse(String nome, String slug) {

    public static PublicTenantResponse from(Tenant tenant) {
        return new PublicTenantResponse(tenant.getNome(), tenant.getSlug());
    }

}
