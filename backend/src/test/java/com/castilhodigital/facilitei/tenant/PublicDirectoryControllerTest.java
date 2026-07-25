package com.castilhodigital.facilitei.tenant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicDirectoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicDirectoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantRepository tenantRepository;

    @Test
    void listarSemFiltrosRetornaTenantsVisiveis() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setNome("Barbearia do Ze");
        tenant.setSlug("barbearia-do-ze");
        tenant.setCategoria(CategoriaNegocio.BARBEARIA);
        tenant.setCidade("Palmas");
        tenant.setDescricao("Cortes e barba");

        when(tenantRepository.buscarNoDiretorio(isNull(), isNull(), isNull())).thenReturn(List.of(tenant));

        mockMvc.perform(get("/api/public/directory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("barbearia-do-ze"))
                .andExpect(jsonPath("$[0].categoria").value("BARBEARIA"))
                .andExpect(jsonPath("$[0].cidade").value("Palmas"));
    }

    @Test
    void listarComFiltrosRepassaOsParametrosParaORepositorioJaComoLikeEmMinusculas() throws Exception {
        when(tenantRepository.buscarNoDiretorio(eq(CategoriaNegocio.SALAO_DE_BELEZA), eq("%palmas%"), eq("%ze%")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/public/directory")
                        .param("categoria", "SALAO_DE_BELEZA")
                        .param("cidade", "Palmas")
                        .param("q", "ze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

}
