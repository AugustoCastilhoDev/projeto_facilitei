package com.castilhodigital.facilitei.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository);
    }

    @Test
    void atualizarDiretorioComCategoriaECidadePreenchidasPermiteFicarVisivel() {
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        Tenant resultado = tenantService.atualizarDiretorio(1L, CategoriaNegocio.BARBEARIA, "Palmas", "Cortes e barba", true);

        assertThat(resultado.getCategoria()).isEqualTo(CategoriaNegocio.BARBEARIA);
        assertThat(resultado.getCidade()).isEqualTo("Palmas");
        assertThat(resultado.isVisivelNoDiretorio()).isTrue();
    }

    @Test
    void atualizarDiretorioVisivelSemCategoriaLancaRegraDeNegocio() {
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.atualizarDiretorio(1L, null, "Palmas", null, true))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void atualizarDiretorioVisivelSemCidadeLancaRegraDeNegocio() {
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.atualizarDiretorio(1L, CategoriaNegocio.BARBEARIA, " ", null, true))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void atualizarDiretorioNaoVisivelNaoExigeCategoriaNemCidade() {
        Tenant tenant = new Tenant();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        Tenant resultado = tenantService.atualizarDiretorio(1L, null, null, null, false);

        assertThat(resultado.isVisivelNoDiretorio()).isFalse();
    }

}
