package com.castilhodigital.facilitei.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TelefoneBrasilTest {

    @ParameterizedTest
    @ValueSource(strings = {"(63) 99999-9999", "63999999999", "5563999999999", "+55 (63) 99999-9999", "55 63 99999 9999"})
    void celularComDddViraOFormatoInternacionalSoComDigitos(String entrada) {
        assertThat(TelefoneBrasil.normalizar(entrada)).isEqualTo("5563999999999");
    }

    @Test
    void telefoneFixoComOitoDigitosTambemEAceito() {
        assertThat(TelefoneBrasil.normalizar("(63) 3212-3456")).isEqualTo("556332123456");
    }

    @ParameterizedTest
    @ValueSource(strings = {"999999999", "123", "44999999999999", "+1 415 555 2671", "abc"})
    void recusaQuemNaoParecerTelefoneBrasileiro(String entrada) {
        assertThatThrownBy(() -> TelefoneBrasil.normalizar(entrada)).isInstanceOf(RegraDeNegocioException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void recusaNuloEVazio(String entrada) {
        assertThatThrownBy(() -> TelefoneBrasil.normalizar(entrada)).isInstanceOf(RegraDeNegocioException.class);
    }

}
