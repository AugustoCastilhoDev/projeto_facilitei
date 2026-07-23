package com.castilhodigital.facilitei.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ServiceOfferingAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceOfferingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceOfferingService serviceOfferingService;

    @MockitoBean
    private ProfissionalService profissionalService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    private ServiceOffering novoServico(Long id, Tenant tenant) {
        ServiceOffering service = new ServiceOffering();
        service.setTenant(tenant);
        service.setNome("Corte");
        service.setDuracaoMin(30);
        service.setPreco(new BigDecimal("50.00"));
        service.setSinalPercentual(new BigDecimal("20.00"));
        service.setAtivo(true);
        ReflectionTestUtils.setField(service, "id", id);
        return service;
    }

    @Test
    void listarRetornaServicosDoTenant() throws Exception {
        Tenant tenant = new Tenant();
        when(serviceOfferingService.listarTodos(1L)).thenReturn(List.of(novoServico(5L, tenant)));

        mockMvc.perform(get("/api/admin/tenants/1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].nome").value("Corte"));
    }

    @Test
    void listarIncluiProfissionaisVinculados() throws Exception {
        Tenant tenant = new Tenant();
        ServiceOffering servico = novoServico(5L, tenant);

        Profissional profissional = new Profissional();
        profissional.setNome("Ana");
        ReflectionTestUtils.setField(profissional, "id", 7L);
        profissional.getServicos().add(servico);

        when(serviceOfferingService.listarTodos(1L)).thenReturn(List.of(servico));
        when(profissionalService.listarTodos(1L)).thenReturn(List.of(profissional));

        mockMvc.perform(get("/api/admin/tenants/1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profissionalIds[0]").value(7))
                .andExpect(jsonPath("$[0].profissionalNomes[0]").value("Ana"));
    }

    @Test
    void criarComDadosValidosRetorna201() throws Exception {
        Tenant tenant = new Tenant();
        when(tenantService.buscarPorId(1L)).thenReturn(tenant);
        when(serviceOfferingService.criar(eq(tenant), any(), anyInt(), any(), any(), any()))
                .thenReturn(novoServico(9L, tenant));

        ServiceOfferingRequest request = new ServiceOfferingRequest(
                "Corte", 30, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of());

        mockMvc.perform(post("/api/admin/tenants/1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void criarComDuracaoInvalidaRetorna400() throws Exception {
        ServiceOfferingRequest request = new ServiceOfferingRequest(
                "Corte", -10, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of());

        mockMvc.perform(post("/api/admin/tenants/1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarServicoInexistenteRetorna404() throws Exception {
        when(serviceOfferingService.atualizar(eq(1L), eq(999L), any(), anyInt(), any(), any(), any()))
                .thenThrow(new EntidadeNaoEncontradaException("Servico nao encontrado (id=999) para este tenant."));

        ServiceOfferingRequest request = new ServiceOfferingRequest(
                "Corte", 30, new BigDecimal("50.00"), new BigDecimal("20.00"), List.of());

        mockMvc.perform(put("/api/admin/tenants/1/services/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void desativarRetorna204() throws Exception {
        mockMvc.perform(delete("/api/admin/tenants/1/services/9"))
                .andExpect(status().isNoContent());
    }

    @Test
    void ativarRetorna204() throws Exception {
        mockMvc.perform(patch("/api/admin/tenants/1/services/9/ativar"))
                .andExpect(status().isNoContent());
    }

}
