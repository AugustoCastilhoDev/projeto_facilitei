package com.castilhodigital.facilitei.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfCnpjUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "24971563792", "11222333000181", "11.222.333/0001-81"})
    void aceitaCpfECnpjComDigitosVerificadoresCorretos(String documento) {
        assertThat(CpfCnpjUtils.valido(documento)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "52998224726",          // CPF com ultimo digito verificador errado
            "52998224735",          // CPF com penultimo digito verificador errado
            "11222333000182",       // CNPJ com ultimo digito verificador errado
            "11111111111",          // CPF com todos os digitos iguais (passa na conta, mas e invalido)
            "00000000000",
            "11111111111111",       // CNPJ com todos os digitos iguais
            "5299822472",           // curto demais
            "529982247250",         // 12 digitos: nem CPF nem CNPJ
            "abc"})
    void rejeitaDocumentosInvalidos(String documento) {
        assertThat(CpfCnpjUtils.valido(documento)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejeitaNuloEVazio(String documento) {
        assertThat(CpfCnpjUtils.valido(documento)).isFalse();
    }

    @Test
    void somenteDigitosRemoveAMascara() {
        assertThat(CpfCnpjUtils.somenteDigitos("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfCnpjUtils.somenteDigitos("11.222.333/0001-81")).isEqualTo("11222333000181");
        assertThat(CpfCnpjUtils.somenteDigitos(null)).isNull();
    }

}
