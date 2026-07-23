package com.castilhodigital.facilitei.billing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Roda uma vez por dia: gera a fatura Pix de quem teve o trial encerrado ou
 * o ciclo mensal vencido, e marca como vencida qualquer fatura pendente cujo
 * prazo passou. Mesmo padrao de BookingExpirationScheduler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssinaturaBillingScheduler {

    private static final long INTERVALO_MS = 24 * 60 * 60 * 1000;

    private final AssinaturaService assinaturaService;

    @Scheduled(fixedRate = INTERVALO_MS)
    public void executar() {
        assinaturaService.gerarFaturasDoDia();
        assinaturaService.marcarFaturasVencidas();
    }

}
