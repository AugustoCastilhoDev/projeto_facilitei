package com.castilhodigital.facilitei.agenda.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador por chave (o IP de quem chama) numa janela fixa: a primeira ocorrencia abre a janela; passado o limite
 * dentro dela, as seguintes sao recusadas ate a janela fechar. Em memoria, por instancia - a mesma limitacao
 * documentada em LoginRateLimiter (varias instancias do backend exigiriam um armazenamento compartilhado).
 *
 * Conta toda tentativa, nao so as que falham: aqui o abuso nao e forca bruta e sim "segurar horarios" e
 * "mandar pedidos de conferencia ao WhatsApp do dono", que sao tentativas que dao certo.
 */
final class JanelaDeLimite {

    /** Teto de chaves guardadas: um atacante trocando de IP nao pode encher a memoria. */
    private static final int MAXIMO_DE_CHAVES = 100_000;

    private final int maximo;
    private final Duration janela;
    private final Clock clock;
    private final Map<String, Contagem> porChave = new ConcurrentHashMap<>();

    JanelaDeLimite(int maximo, Duration janela, Clock clock) {
        this.maximo = maximo;
        this.janela = janela;
        this.clock = clock;
    }

    private record Contagem(Instant inicioDaJanela, int total) {
    }

    /** Conta esta ocorrencia; devolve false se ela ja passou do limite da janela. */
    boolean permitir(String chave) {
        Instant agora = clock.instant();
        if (porChave.size() >= MAXIMO_DE_CHAVES) {
            limparExpirados();
        }
        Contagem contagem = porChave.compute(chave, (k, atual) -> atual == null || janelaFechada(atual, agora)
                ? new Contagem(agora, 1)
                : new Contagem(atual.inicioDaJanela(), atual.total() + 1));
        return contagem.total() <= maximo;
    }

    void limparExpirados() {
        Instant agora = clock.instant();
        porChave.entrySet().removeIf(entrada -> janelaFechada(entrada.getValue(), agora));
    }

    int chavesGuardadas() {
        return porChave.size();
    }

    private boolean janelaFechada(Contagem contagem, Instant agora) {
        return !contagem.inicioDaJanela().plus(janela).isAfter(agora);
    }

}
