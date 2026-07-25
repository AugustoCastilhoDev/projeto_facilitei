package com.castilhodigital.facilitei.tenant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(TenantDiretorioAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantDiretorioAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    @Test
    void consultarRetornaConfiguracaoAtual() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setCategoria(CategoriaNegocio.BARBEARIA);
        tenant.setCidade("Palmas");
        tenant.setVisivelNoDiretorio(true);
        when(tenantService.buscarPorId(1L)).thenReturn(tenant);

        mockMvc.perform(get("/api/admin/tenants/1/diretorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("BARBEARIA"))
                .andExpect(jsonPath("$.cidade").value("Palmas"))
                .andExpect(jsonPath("$.visivelNoDiretorio").value(true));
    }

    @Test
    void atualizarComDadosValidosRetornaConfiguracaoAtualizada() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setCategoria(CategoriaNegocio.ESTETICA);
        tenant.setCidade("Palmas");
        tenant.setDescricao("Cuidados com a pele");
        tenant.setVisivelNoDiretorio(true);
        when(tenantService.atualizarDiretorio(
                        eq(1L), eq(CategoriaNegocio.ESTETICA), eq("Palmas"), eq("Cuidados com a pele"), eq(true)))
                .thenReturn(tenant);

        AtualizarDiretorioRequest request =
                new AtualizarDiretorioRequest(CategoriaNegocio.ESTETICA, "Palmas", "Cuidados com a pele", true);

        mockMvc.perform(put("/api/admin/tenants/1/diretorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("ESTETICA"))
                .andExpect(jsonPath("$.visivelNoDiretorio").value(true));
    }

}
