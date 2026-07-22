package com.castilhodigital.facilitei.professional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicProfissionalController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicProfissionalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfissionalService profissionalService;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void listarRetornaProfissionaisAtivosQueRealizamOServico() throws Exception {
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1L);

        Profissional profissional = new Profissional();
        profissional.setNome("Joana");
        profissional.setHorarioAbertura(LocalTime.of(9, 0));
        profissional.setHorarioFechamento(LocalTime.of(18, 0));
        ReflectionTestUtils.setField(profissional, "id", 5L);

        when(tenantService.buscarPorSlug("barbearia-do-ze")).thenReturn(tenant);
        when(profissionalService.listarAtivosPorServico(1L, 7L)).thenReturn(List.of(profissional));

        mockMvc.perform(get("/api/public/tenants/barbearia-do-ze/profissionais").param("serviceId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].nome").value("Joana"));
    }

    @Test
    void listarSemParametroServiceIdRetorna400() throws Exception {
        mockMvc.perform(get("/api/public/tenants/barbearia-do-ze/profissionais"))
                .andExpect(status().isBadRequest());
    }

}
