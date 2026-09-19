package com.castilhodigital.facilitei.pix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class ChavePixTest {

    @Test
    void cpfEcnpjSaemSoComDigitos() {
        assertThat(ChavePix.normalizar(TipoChavePix.CPF, "529.982.247-25")).isEqualTo("52998224725");
        assertThat(ChavePix.normalizar(TipoChavePix.CNPJ, "11.222.333/0001-81")).isEqualTo("11222333000181");
    }

    @Test
    void cpfEcnpjPrecisamTerDigitoVerificadorCertoETamanhoDoTipoEscolhido() {
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.CPF, "529.982.247-26"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("CPF");
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.CNPJ, "11.222.333/0001-82"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("CNPJ");
        // um CNPJ valido nao serve como chave "CPF" (e vice-versa): o dono escolheu o tipo errado
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.CPF, "11222333000181"))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.CNPJ, "52998224725"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    /** O BR Code exige "+55" na chave de telefone; o dono digita como escreve no WhatsApp. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            (63) 99999-9999    | +5563999999999
            63999999999        | +5563999999999
            5563999999999      | +5563999999999
            +55 63 99999-9999  | +5563999999999
            (63) 3212-3456     | +556332123456
            """)
    void telefoneVaiComMaisCinquentaECinco(String digitado, String esperado) {
        assertThat(ChavePix.normalizar(TipoChavePix.TELEFONE, digitado)).isEqualTo(esperado);
    }

    @Test
    void telefoneInvalidoERecusado() {
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.TELEFONE, "12345"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void emailFicaEmMinusculasEPrecisaTerFormatoValido() {
        assertThat(ChavePix.normalizar(TipoChavePix.EMAIL, "  Dono@Barbearia.com.BR ")).isEqualTo("dono@barbearia.com.br");
        for (String invalido : new String[] {"sem-arroba", "dono@", "@barbearia.com", "dono@barbearia", "do no@x.com"}) {
            assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.EMAIL, invalido))
                    .as(invalido).isInstanceOf(RegraDeNegocioException.class);
        }
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.EMAIL, "a".repeat(70) + "@barbearia.com"))
                .as("nao cabe no campo 26 do BR Code").isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void chaveAleatoriaEUmUuidEmMinusculas() {
        assertThat(ChavePix.normalizar(TipoChavePix.ALEATORIA, "123E4567-E89B-12D3-A456-426614174000"))
                .isEqualTo("123e4567-e89b-12d3-a456-426614174000");
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.ALEATORIA, "123e4567e89b12d3a456426614174000"))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> ChavePix.normalizar(TipoChavePix.ALEATORIA, "isto-nao-e-um-uuid"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @ParameterizedTest
    @EnumSource(TipoChavePix.class)
    void valorEmBrancoNuncaEValido(TipoChavePix tipo) {
        assertThatThrownBy(() -> ChavePix.normalizar(tipo, "  ")).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> ChavePix.normalizar(tipo, null)).isInstanceOf(RegraDeNegocioException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void semTipoNaoHaComoValidar(String valor) {
        assertThatThrownBy(() -> ChavePix.normalizar(null, valor)).isInstanceOf(RegraDeNegocioException.class);
    }

}
