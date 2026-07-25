package com.castilhodigital.facilitei.tenant;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diretorio publico de negocios (portal do cliente) - complemento ao link
 * direto /agendar/:slug, nao substituto: so lista tenants que marcaram
 * visivelNoDiretorio = true (ver TenantService.atualizarDiretorio).
 */
@RestController
@RequestMapping("/api/public/directory")
@RequiredArgsConstructor
public class PublicDirectoryController {

    private final TenantRepository tenantRepository;

    @GetMapping
    public List<DiretorioTenantResponse> listar(
            @RequestParam(required = false) CategoriaNegocio categoria,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String q) {
        return tenantRepository.buscarNoDiretorio(categoria, paraLike(cidade), paraLike(q)).stream()
                .map(DiretorioTenantResponse::from)
                .toList();
    }

    /** Nulo/branco vira null (filtro ignorado); senao, ja monta o padrao LIKE em minusculas - ver TenantRepository.buscarNoDiretorio. */
    private String paraLike(String valor) {
        return (valor == null || valor.isBlank()) ? null : "%" + valor.toLowerCase() + "%";
    }

}
