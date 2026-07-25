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
 * Limitador de abuso do fluxo de OTP, mesmo desenho de LoginRateLimiter
 * (janela deslizante em memoria, por instancia - mesma limitacao
 * single-instance documentada la, sem Redis) mas com duas dimensoes
 * independentes, chaveadas por telefone:
 *
 * - envio: evita que alguem gere codigos (e dispare mensagens WhatsApp de
 *   verdade, com custo real no provedor) em loop.
 * - verificacao: evita forca bruta contra um codigo especifico tentando
 *   varios telefones/janelas alem do limite de tentativas ja embutido em
 *   CodigoVerificacao.tentativas (aquele e por-codigo; este e por-telefone
 *   entre codigos diferentes).
 */
@Component
public class OtpRateLimiter {

    private static final int MAX_ENVIOS = 3;
    private static final Duration JANELA_ENVIO = Duration.ofHours(1);

    private static final int MAX_FALHAS_VERIFICACAO = 5;
    private static final Duration JANELA_VERIFICACAO = Duration.ofMinutes(5);

    private final Map<String, Estado> enviosPorTelefone = new ConcurrentHashMap<>();
    private final Map<String, Estado> falhasVerificacaoPorTelefone = new ConcurrentHashMap<>();

    public void verificarLimiteEnvio(String telefone) {
        verificarLimite(enviosPorTelefone, telefone, MAX_ENVIOS, JANELA_ENVIO,
                "Muitos codigos solicitados para este telefone. Aguarde antes de tentar novamente.");
    }

    public void registrarEnvio(String telefone) {
        registrarOcorrencia(enviosPorTelefone, telefone, JANELA_ENVIO);
    }

    public void verificarLimiteVerificacao(String telefone) {
        verificarLimite(falhasVerificacaoPorTelefone, telefone, MAX_FALHAS_VERIFICACAO, JANELA_VERIFICACAO,
                "Muitas tentativas de verificacao. Aguarde um instante antes de tentar novamente.");
    }

    public void registrarFalhaVerificacao(String telefone) {
        registrarOcorrencia(falhasVerificacaoPorTelefone, telefone, JANELA_VERIFICACAO);
    }

    public void registrarSucessoVerificacao(String telefone) {
        falhasVerificacaoPorTelefone.remove(telefone);
    }

    private void verificarLimite(Map<String, Estado> mapa, String chave, int maxOcorrencias, Duration janela, String mensagem) {
        Estado estado = mapa.get(chave);
        if (estado == null || janelaExpirada(estado, janela)) {
            return;
        }
        if (estado.ocorrencias.get() >= maxOcorrencias) {
            throw new LimiteDeRequisicoesExcedidoException(mensagem);
        }
    }

    private void registrarOcorrencia(Map<String, Estado> mapa, String chave, Duration janela) {
        mapa.compute(chave, (k, atual) -> {
            if (atual == null || janelaExpirada(atual, janela)) {
                return new Estado(Instant.now());
            }
            atual.ocorrencias.incrementAndGet();
            return atual;
        });
    }

    private boolean janelaExpirada(Estado estado, Duration janela) {
        return estado.inicioJanela.plus(janela).isBefore(Instant.now());
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    void limparEntradasExpiradas() {
        enviosPorTelefone.entrySet().removeIf(entry -> janelaExpirada(entry.getValue(), JANELA_ENVIO));
        falhasVerificacaoPorTelefone.entrySet().removeIf(entry -> janelaExpirada(entry.getValue(), JANELA_VERIFICACAO));
    }

    private static final class Estado {
        private final Instant inicioJanela;
        private final AtomicInteger ocorrencias = new AtomicInteger(1);

        private Estado(Instant inicioJanela) {
            this.inicioJanela = inicioJanela;
        }
    }

}
