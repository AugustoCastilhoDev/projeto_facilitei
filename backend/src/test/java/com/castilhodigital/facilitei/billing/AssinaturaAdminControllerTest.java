package com.castilhodigital.facilitei.billing;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AssinaturaAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssinaturaAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssinaturaService assinaturaService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    @Test
    void consultarRetornaStatusDaAssinatura() throws Exception {
        when(assinaturaService.consultarStatus(1L)).thenReturn(new AssinaturaResponse(
                Plano.BASICO, AssinaturaStatus.TRIAL, LocalDate.of(2026, 8, 20), null, null));

        mockMvc.perform(get("/api/admin/tenants/1/assinatura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("BASICO"))
                .andExpect(jsonPath("$.status").value("TRIAL"))
                .andExpect(jsonPath("$.trialAte").value("2026-08-20"));
    }

    @Test
    void cancelarRetorna204() throws Exception {
        mockMvc.perform(patch("/api/admin/tenants/1/assinatura/cancelar"))
                .andExpect(status().isNoContent());

        verify(assinaturaService).cancelar(1L);
    }

    @Test
    void consultarComTenantMismatchRetorna403() throws Exception {
        doThrow(new AcessoNegadoException("Acesso negado a este tenant."))
                .when(tenantSecurityGuard).verificarAcessoAoTenant(eq(1L));

        mockMvc.perform(get("/api/admin/tenants/1/assinatura"))
                .andExpect(status().isForbidden());
    }

}
