package com.castilhodigital.facilitei.agenda;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Devolve para a agenda os horarios segurados cujo prazo acabou sem pagamento.
 * Roda a cada minuto porque o prazo e curto (minutos): quanto mais tarde o job,
 * mais tempo um horario fica preso sem necessidade.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AgendamentoExpiracaoScheduler {

    private static final long INTERVALO_MS = 60 * 1000;

    private final AgendamentoService agendamentoService;
    private final Clock clock;

    @Scheduled(fixedRate = INTERVALO_MS)
    void expirarVencidos() {
        int expirados = agendamentoService.expirarVencidos(Instant.now(clock));
        if (expirados > 0) {
            log.info("{} agendamento(s) expirado(s) por falta de pagamento do sinal.", expirados);
        }
    }

}
