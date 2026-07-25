package com.castilhodigital.facilitei.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import org.junit.jupiter.api.Test;

class OtpRateLimiterTest {

    private final OtpRateLimiter rateLimiter = new OtpRateLimiter();

    @Test
    void permiteEnviosAbaixoDoLimite() {
        String telefone = "5563999990001";
        rateLimiter.registrarEnvio(telefone);
        rateLimiter.registrarEnvio(telefone);

        assertThatCode(() -> rateLimiter.verificarLimiteEnvio(telefone)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaEnvioAposAtingirOLimite() {
        String telefone = "5563999990002";
        rateLimiter.registrarEnvio(telefone);
        rateLimiter.registrarEnvio(telefone);
        rateLimiter.registrarEnvio(telefone);

        assertThatThrownBy(() -> rateLimiter.verificarLimiteEnvio(telefone))
                .isInstanceOf(LimiteDeRequisicoesExcedidoException.class);
    }

    @Test
    void bloqueiaVerificacaoAposAtingirOLimiteDeFalhas() {
        String telefone = "5563999990003";
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFalhaVerificacao(telefone);
        }

        assertThatThrownBy(() -> rateLimiter.verificarLimiteVerificacao(telefone))
                .isInstanceOf(LimiteDeRequisicoesExcedidoException.class);
    }

    @Test
    void sucessoNaVerificacaoLimpaOEstadoDeFalhas() {
        String telefone = "5563999990004";
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFalhaVerificacao(telefone);
        }

        rateLimiter.registrarSucessoVerificacao(telefone);

        assertThatCode(() -> rateLimiter.verificarLimiteVerificacao(telefone)).doesNotThrowAnyException();
    }

    @Test
    void limitesDeEnvioENaoAfetamOutroTelefone() {
        String telefoneBloqueado = "5563999990005";
        String outroTelefone = "5563999990006";
        rateLimiter.registrarEnvio(telefoneBloqueado);
        rateLimiter.registrarEnvio(telefoneBloqueado);
        rateLimiter.registrarEnvio(telefoneBloqueado);

        assertThatCode(() -> rateLimiter.verificarLimiteEnvio(outroTelefone)).doesNotThrowAnyException();
    }

}
