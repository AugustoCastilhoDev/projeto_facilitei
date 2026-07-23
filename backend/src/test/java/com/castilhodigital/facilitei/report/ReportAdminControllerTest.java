package com.castilhodigital.facilitei.report;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.common.exception.AcessoNegadoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    @Test
    void gerarRetornaRelatorioDoPeriodo() throws Exception {
        LocalDate inicio = LocalDate.now().minusDays(7);
        LocalDate fim = LocalDate.now();
        RelatorioResponse relatorio = new RelatorioResponse(
                new BigDecimal("100.00"), 2, 1, 0, BigDecimal.ZERO, List.of());
        when(reportService.gerarRelatorio(1L, inicio, fim)).thenReturn(relatorio);

        mockMvc.perform(get("/api/admin/tenants/1/relatorios")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamentoTotal").value(100.00))
                .andExpect(jsonPath("$.totalReservasConfirmadas").value(2));
    }

    @Test
    void gerarComFimAnteriorAoInicioRetorna400() throws Exception {
        LocalDate inicio = LocalDate.now();
        LocalDate fim = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/admin/tenants/1/relatorios")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acessoATenantDeOutroDonoRetorna403() throws Exception {
        LocalDate hoje = LocalDate.now();
        doThrow(new AcessoNegadoException("Voce nao tem acesso aos dados deste tenant."))
                .when(tenantSecurityGuard).verificarAcessoAoTenant(2L);

        mockMvc.perform(get("/api/admin/tenants/2/relatorios")
                        .param("inicio", hoje.toString())
                        .param("fim", hoje.toString()))
                .andExpect(status().isForbidden());
    }

}
