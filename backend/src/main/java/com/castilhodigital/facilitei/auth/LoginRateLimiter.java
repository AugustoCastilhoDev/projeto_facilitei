package com.castilhodigital.facilitei.auth;

import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bloqueio simples de forca bruta no login: cada chave (IP do cliente) tem uma
 * janela deslizante de tentativas com falha; ao atingir o limite, novas
 * tentativas sao rejeitadas ate a janela expirar. Estado em memoria, por
 * instancia - suficiente para um unico backend; um deploy com multiplas
 * instancias exigiria um armazenamento compartilhado (ex.: Redis).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private final Map<String, Estado> estadoPorChave = new ConcurrentHashMap<>();

    public void verificarLimite(String chave) {
        Estado estado = estadoPorChave.get(chave);
        if (estado == null || janelaExpirada(estado)) {
            return;
        }
        if (estado.falhas.get() >= MAX_FALHAS) {
            throw new LimiteDeRequisicoesExcedidoException(
                    "Muitas tentativas de login. Aguarde um instante antes de tentar novamente.");
        }
    }

    public void registrarFalha(String chave) {
        estadoPorChave.compute(chave, (k, atual) -> {
            if (atual == null || janelaExpirada(atual)) {
                return new Estado(Instant.now());
            }
            atual.falhas.incrementAndGet();
            return atual;
        });
    }

    public void registrarSucesso(String chave) {
        estadoPorChave.remove(chave);
    }

    private boolean janelaExpirada(Estado estado) {
        return estado.inicioJanela.plus(JANELA).isBefore(Instant.now());
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    void limparEntradasExpiradas() {
        estadoPorChave.entrySet().removeIf(entry -> janelaExpirada(entry.getValue()));
    }

    private static final class Estado {
        private final Instant inicioJanela;
        private final AtomicInteger falhas = new AtomicInteger(1);

        private Estado(Instant inicioJanela) {
            this.inicioJanela = inicioJanela;
        }
    }

}
