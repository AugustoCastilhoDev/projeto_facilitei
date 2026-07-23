package com.castilhodigital.facilitei.report;

import com.castilhodigital.facilitei.booking.Booking;
import com.castilhodigital.facilitei.booking.BookingRepository;
import com.castilhodigital.facilitei.booking.PaymentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agregacao feita em memoria com Streams, nao com uma query GROUP BY: o
 * volume de reservas confirmadas por tenant/periodo e pequeno o bastante
 * (dezenas a poucas centenas por mes) para nao justificar a primeira query
 * de agregacao SQL do projeto - JOIN FETCH simples + Streams e mais legivel.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");
    private static final List<PaymentStatus> STATUS_CONFIRMADOS = List.of(PaymentStatus.PAGO, PaymentStatus.SEM_SINAL);

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public RelatorioResponse gerarRelatorio(Long tenantId, LocalDate dataInicio, LocalDate dataFim) {
        OffsetDateTime inicio = dataInicio.atStartOfDay(ZONE_ID).toOffsetDateTime();
        OffsetDateTime fim = dataFim.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime();
        List<Booking> bookings = bookingRepository.findParaRelatorio(tenantId, STATUS_CONFIRMADOS, inicio, fim);

        BigDecimal faturamentoTotal = calcularFaturamento(bookings);

        List<Booking> marcados = bookings.stream().filter(b -> b.getCompareceu() != null).toList();
        long naoComparecimentos = marcados.stream().filter(b -> !b.getCompareceu()).count();
        BigDecimal taxaNaoComparecimento = calcularTaxa(naoComparecimentos, marcados.size());

        List<ClienteRecorrenteResponse> clientesRecorrentes = calcularClientesRecorrentes(bookings);

        return new RelatorioResponse(
                faturamentoTotal,
                bookings.size(),
                marcados.size(),
                (int) naoComparecimentos,
                taxaNaoComparecimento,
                clientesRecorrentes);
    }

    /**
     * Conta o valor cheio do servico para quem compareceu OU ainda nao foi
     * marcado (nao da para saber retroativamente) - so desconta quem foi
     * confirmadamente marcado como nao comparecimento. Nao distingue o sinal
     * ja recebido via Pix do valor total combinado a receber no local (ver
     * limitacao no README).
     */
    private BigDecimal calcularFaturamento(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> !Boolean.FALSE.equals(b.getCompareceu()))
                .map(b -> b.getSlot().getService().getPreco())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularTaxa(long naoComparecimentos, int totalMarcados) {
        if (totalMarcados == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(naoComparecimentos * 100)
                .divide(BigDecimal.valueOf(totalMarcados), 1, RoundingMode.HALF_UP);
    }

    private List<ClienteRecorrenteResponse> calcularClientesRecorrentes(List<Booking> bookings) {
        return bookings.stream()
                .collect(Collectors.groupingBy(Booking::getClienteTelefone))
                .values().stream()
                .filter(grupo -> grupo.size() > 1)
                .map(grupo -> {
                    Booking maisRecente = grupo.stream()
                            .max(Comparator.comparing(b -> b.getSlot().getDataHora()))
                            .orElseThrow();
                    return new ClienteRecorrenteResponse(
                            maisRecente.getClienteNome(),
                            maisRecente.getClienteTelefone(),
                            grupo.size(),
                            maisRecente.getSlot().getDataHora());
                })
                .sorted(Comparator.comparing(ClienteRecorrenteResponse::totalAgendamentos).reversed())
                .toList();
    }

}
