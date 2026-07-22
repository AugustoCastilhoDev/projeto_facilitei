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

@WebMvcTest(TenantAsaasConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantAsaasConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    @Test
    void consultarTenantSemChaveConfiguradaRetornaConfiguradoFalso() throws Exception {
        Tenant tenant = new Tenant();
        when(tenantService.buscarPorId(1L)).thenReturn(tenant);

        mockMvc.perform(get("/api/admin/tenants/1/asaas-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurado").value(false))
                .andExpect(jsonPath("$.webhookUrl").value(org.hamcrest.Matchers.endsWith("/api/webhooks/asaas")));
    }

    @Test
    void atualizarComChaveValidaRetornaConfiguradoVerdadeiroComWebhookToken() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setAsaasApiKey("chave-real");
        tenant.setAsaasWebhookToken("token-gerado-123");
        when(tenantService.configurarAsaas(eq(1L), eq("chave-real"))).thenReturn(tenant);

        AtualizarAsaasApiKeyRequest request = new AtualizarAsaasApiKeyRequest("chave-real");

        mockMvc.perform(put("/api/admin/tenants/1/asaas-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurado").value(true))
                .andExpect(jsonPath("$.webhookToken").value("token-gerado-123"));
    }

    @Test
    void atualizarComChaveEmBrancoRetorna400() throws Exception {
        AtualizarAsaasApiKeyRequest request = new AtualizarAsaasApiKeyRequest("  ");

        mockMvc.perform(put("/api/admin/tenants/1/asaas-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}
