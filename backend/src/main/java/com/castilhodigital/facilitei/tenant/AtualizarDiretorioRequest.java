package com.castilhodigital.facilitei.tenant;

public record AtualizarDiretorioRequest(
        CategoriaNegocio categoria,
        String cidade,
        String descricao,
        boolean visivelNoDiretorio
) {
}
