package com.castilhodigital.facilitei.tenant;

public record DiretorioConfigResponse(
        CategoriaNegocio categoria,
        String cidade,
        String descricao,
        boolean visivelNoDiretorio
) {

    public static DiretorioConfigResponse from(Tenant tenant) {
        return new DiretorioConfigResponse(
                tenant.getCategoria(), tenant.getCidade(), tenant.getDescricao(), tenant.isVisivelNoDiretorio());
    }

}
