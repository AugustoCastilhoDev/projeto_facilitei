package com.castilhodigital.facilitei.billing;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import org.junit.jupiter.api.Test;

class AssinaturaGuardTest {

    private final AssinaturaGuard guard = new AssinaturaGuard();

    private Tenant tenant(AssinaturaStatus status, Plano plano) {
        Tenant tenant = new Tenant();
        tenant.setAssinaturaStatus(status);
        tenant.setPlano(plano);
        return tenant;
    }

    @Test
    void permiteUsoQuandoTrial() {
        assertThatCode(() -> guard.verificarUsoLiberado(tenant(AssinaturaStatus.TRIAL, Plano.BASICO)))
                .doesNotThrowAnyException();
    }

    @Test
    void permiteUsoQuandoAtiva() {
        assertThatCode(() -> guard.verificarUsoLiberado(tenant(AssinaturaStatus.ATIVA, Plano.BASICO)))
                .doesNotThrowAnyException();
    }

    @Test
    void bloqueiaUsoQuandoInadimplente() {
        assertThatThrownBy(() -> guard.verificarUsoLiberado(tenant(AssinaturaStatus.INADIMPLENTE, Plano.BASICO)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void bloqueiaUsoQuandoCancelada() {
        assertThatThrownBy(() -> guard.verificarUsoLiberado(tenant(AssinaturaStatus.CANCELADA, Plano.BASICO)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void permiteCriarProfissionalAbaixoDoLimite() {
        assertThatCode(() -> guard.verificarLimiteProfissionais(tenant(AssinaturaStatus.ATIVA, Plano.BASICO), 1))
                .doesNotThrowAnyException();
    }

    @Test
    void bloqueiaCriarProfissionalNoLimiteDoPlano() {
        assertThatThrownBy(() -> guard.verificarLimiteProfissionais(tenant(AssinaturaStatus.ATIVA, Plano.BASICO), 2))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Limite");
    }

}
