package com.castilhodigital.facilitei.tenant;

public record DiretorioTenantResponse(
        String nome,
        String slug,
        CategoriaNegocio categoria,
        String cidade,
        String descricao
) {

    public static DiretorioTenantResponse from(Tenant tenant) {
        return new DiretorioTenantResponse(
                tenant.getNome(), tenant.getSlug(), tenant.getCategoria(), tenant.getCidade(), tenant.getDescricao());
    }

}
