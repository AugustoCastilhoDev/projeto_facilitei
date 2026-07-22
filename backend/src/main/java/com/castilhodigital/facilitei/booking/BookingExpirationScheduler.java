package com.castilhodigital.facilitei.booking;

import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Verifica periodicamente reservas pendentes cujo horario do slot esta
 * proximo (ou ja passou) e as expira, liberando o horario. Complementa o
 * webhook PAYMENT_OVERDUE da Asaas (BookingService.marcarComoExpirado), que
 * so tem granularidade de dia e nao garante que a reserva expire antes do
 * proprio compromisso acontecer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    /** Antecedencia escolhida para negocios de servico (barbearia, salao etc.) - da tempo de pagar sem travar a agenda. */
    private static final Duration ANTECEDENCIA_MINIMA = Duration.ofHours(1);

    private static final long INTERVALO_VERIFICACAO_MS = 5 * 60 * 1000;

    private final BookingService bookingService;

    @Scheduled(fixedRate = INTERVALO_VERIFICACAO_MS)
    public void expirarReservasProximasDoHorario() {
        OffsetDateTime limite = OffsetDateTime.now().plus(ANTECEDENCIA_MINIMA);
        int quantidade = bookingService.expirarReservasProximasDoHorario(limite);

        if (quantidade > 0) {
            log.info("{} reserva(s) pendente(s) expirada(s) por proximidade do horario do slot.", quantidade);
        }
    }

}
