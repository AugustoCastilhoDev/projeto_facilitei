package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenConfirmacaoTest {

    @Test
    void tokenTem256BitsEmBase64UrlEOHashESha256EmHexadecimal() {
        TokenConfirmacao.Gerado gerado = TokenConfirmacao.gerar();

        assertThat(gerado.valor()).matches("[A-Za-z0-9_-]{43}");          // 32 bytes sem preenchimento
        assertThat(gerado.hash()).matches("[0-9a-f]{64}").isNotEqualTo(gerado.valor());
        assertThat(TokenConfirmacao.hash(gerado.valor())).isEqualTo(gerado.hash());
    }

    /** SHA-256("abc"), o vetor de teste publico do algoritmo. */
    @Test
    void hashBateComOVetorPublicoDoSha256() {
        assertThat(TokenConfirmacao.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void cadaTokenGeradoEDiferente() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertThat(vistos.add(TokenConfirmacao.gerar().valor())).isTrue();
        }
    }

    /** O codigo publico do agendamento (um UUID, que o cliente conhece) nunca pode ser aceito no lugar do token do dono. */
    @Test
    void soAceitaOFormatoQueGerarProduz() {
        assertThat(TokenConfirmacao.formatoValido(TokenConfirmacao.gerar().valor())).isTrue();

        assertThat(TokenConfirmacao.formatoValido(java.util.UUID.randomUUID().toString())).isFalse();
        assertThat(TokenConfirmacao.formatoValido(null)).isFalse();
        assertThat(TokenConfirmacao.formatoValido("")).isFalse();
        assertThat(TokenConfirmacao.formatoValido("a".repeat(42))).isFalse();
        assertThat(TokenConfirmacao.formatoValido("a".repeat(44))).isFalse();
        assertThat(TokenConfirmacao.formatoValido("a".repeat(42) + "=")).isFalse();
        assertThat(TokenConfirmacao.formatoValido("a".repeat(42) + "/")).isFalse();
    }

}
