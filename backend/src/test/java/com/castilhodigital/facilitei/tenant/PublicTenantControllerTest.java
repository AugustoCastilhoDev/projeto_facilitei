package com.castilhodigital.facilitei.tenant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicTenantController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void buscarPorSlugRetornaNomeESlug() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setNome("Barbearia do Ze");
        tenant.setSlug("barbearia-do-ze");
        when(tenantService.buscarPorSlug("barbearia-do-ze")).thenReturn(tenant);

        mockMvc.perform(get("/api/public/tenants/barbearia-do-ze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Barbearia do Ze"))
                .andExpect(jsonPath("$.slug").value("barbearia-do-ze"));
    }

    @Test
    void buscarPorSlugInexistenteRetorna404() throws Exception {
        when(tenantService.buscarPorSlug("nao-existe"))
                .thenThrow(new EntidadeNaoEncontradaException("Tenant nao encontrado para o slug 'nao-existe'."));

        mockMvc.perform(get("/api/public/tenants/nao-existe"))
                .andExpect(status().isNotFound());
    }

}
