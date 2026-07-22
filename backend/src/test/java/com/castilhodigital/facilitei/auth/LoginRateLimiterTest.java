package com.castilhodigital.facilitei.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private final LoginRateLimiter rateLimiter = new LoginRateLimiter();

    @Test
    void permiteTentativasAbaixoDoLimite() {
        String chave = "10.0.0.1";
        for (int i = 0; i < 4; i++) {
            rateLimiter.registrarFalha(chave);
        }

        assertThatCode(() -> rateLimiter.verificarLimite(chave)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaAposAtingirOLimiteDeFalhas() {
        String chave = "10.0.0.2";
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFalha(chave);
        }

        assertThatThrownBy(() -> rateLimiter.verificarLimite(chave))
                .isInstanceOf(LimiteDeRequisicoesExcedidoException.class);
    }

    @Test
    void naoAfetaOutrasChaves() {
        String chaveBloqueada = "10.0.0.3";
        String outraChave = "10.0.0.4";
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFalha(chaveBloqueada);
        }

        assertThatCode(() -> rateLimiter.verificarLimite(outraChave)).doesNotThrowAnyException();
    }

    @Test
    void sucessoLimpaOEstadoDeFalhas() {
        String chave = "10.0.0.5";
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFalha(chave);
        }

        rateLimiter.registrarSucesso(chave);

        assertThatCode(() -> rateLimiter.verificarLimite(chave)).doesNotThrowAnyException();
    }

}
