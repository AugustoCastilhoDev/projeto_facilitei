package com.castilhodigital.facilitei.professional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.TenantSecurityGuard;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.time.LocalTime;
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

@WebMvcTest(ProfissionalAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfissionalAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfissionalService profissionalService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TenantSecurityGuard tenantSecurityGuard;

    private Profissional novoProfissional(Long id) {
        Profissional profissional = new Profissional();
        profissional.setNome("Joana");
        profissional.setHorarioAbertura(LocalTime.of(9, 0));
        profissional.setHorarioFechamento(LocalTime.of(18, 0));
        profissional.setAtivo(true);
        ReflectionTestUtils.setField(profissional, "id", id);
        return profissional;
    }

    @Test
    void listarRetornaProfissionaisDoTenant() throws Exception {
        when(profissionalService.listarTodos(1L)).thenReturn(List.of(novoProfissional(5L)));

        mockMvc.perform(get("/api/admin/tenants/1/profissionais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].nome").value("Joana"));
    }

    @Test
    void criarComDadosValidosRetorna201() throws Exception {
        Tenant tenant = new Tenant();
        when(tenantService.buscarPorId(1L)).thenReturn(tenant);
        when(profissionalService.criar(eq(tenant), any(), any(), any(), any()))
                .thenReturn(novoProfissional(9L));

        ProfissionalRequest request = new ProfissionalRequest("Joana", LocalTime.of(9, 0), LocalTime.of(18, 0), List.of());

        mockMvc.perform(post("/api/admin/tenants/1/profissionais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void criarComNomeEmBrancoRetorna400() throws Exception {
        ProfissionalRequest request = new ProfissionalRequest("", LocalTime.of(9, 0), LocalTime.of(18, 0), List.of());

        mockMvc.perform(post("/api/admin/tenants/1/profissionais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desativarRetorna204() throws Exception {
        mockMvc.perform(delete("/api/admin/tenants/1/profissionais/9"))
                .andExpect(status().isNoContent());
    }

    @Test
    void ativarRetorna204() throws Exception {
        mockMvc.perform(patch("/api/admin/tenants/1/profissionais/9/ativar"))
                .andExpect(status().isNoContent());
    }

}
