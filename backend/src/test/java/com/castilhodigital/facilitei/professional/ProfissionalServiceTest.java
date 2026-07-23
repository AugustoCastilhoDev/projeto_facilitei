package com.castilhodigital.facilitei.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.billing.AssinaturaGuard;
import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.catalog.ServiceOfferingService;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private ServiceOfferingService serviceOfferingService;

    @Mock
    private AssinaturaGuard assinaturaGuard;

    private ProfissionalService profissionalService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        profissionalService = new ProfissionalService(profissionalRepository, serviceOfferingService, assinaturaGuard);
        tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1L);
    }

    @Test
    void criarComHorarioInvalidoLancaExcecao() {
        assertThatThrownBy(() -> profissionalService.criar(
                tenant, "Joana", LocalTime.of(18, 0), LocalTime.of(9, 0), List.of()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void criarComServicosValidosFunciona() {
        ServiceOffering servico = new ServiceOffering();
        ReflectionTestUtils.setField(servico, "id", 7L);

        when(serviceOfferingService.buscarPorIdETenant(1L, 7L)).thenReturn(servico);
        when(profissionalRepository.save(org.mockito.ArgumentMatchers.any(Profissional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Profissional profissional = profissionalService.criar(
                tenant, "Joana", LocalTime.of(9, 0), LocalTime.of(18, 0), List.of(7L));

        assertThat(profissional.getNome()).isEqualTo("Joana");
        assertThat(profissional.getServicos()).containsExactly(servico);
        assertThat(profissional.isAtivo()).isTrue();
    }

    @Test
    void criarComAssinaturaBloqueadaLancaExcecao() {
        org.mockito.Mockito.doThrow(new RegraDeNegocioException("Assinatura cancelada - regularize."))
                .when(assinaturaGuard).verificarUsoLiberado(tenant);

        assertThatThrownBy(() -> profissionalService.criar(
                tenant, "Joana", LocalTime.of(9, 0), LocalTime.of(18, 0), List.of()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Assinatura");
    }

    @Test
    void criarComLimiteDeProfissionaisAtingidoLancaExcecao() {
        when(profissionalRepository.findByTenantIdAndAtivoTrueOrderByNome(1L)).thenReturn(List.of(new Profissional(), new Profissional()));
        org.mockito.Mockito.doThrow(new RegraDeNegocioException("Limite de profissionais do plano atingido."))
                .when(assinaturaGuard).verificarLimiteProfissionais(tenant, 2);

        assertThatThrownBy(() -> profissionalService.criar(
                tenant, "Joana", LocalTime.of(9, 0), LocalTime.of(18, 0), List.of()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Limite");
    }

    @Test
    void buscarPorIdETenantInexistenteLancaExcecao() {
        when(profissionalRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profissionalService.buscarPorIdETenant(1L, 99L))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void desativarMarcaComoInativo() {
        Profissional profissional = new Profissional();
        profissional.setAtivo(true);
        when(profissionalRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(profissional));

        profissionalService.desativar(1L, 5L);

        assertThat(profissional.isAtivo()).isFalse();
    }

}
