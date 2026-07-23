package com.castilhodigital.facilitei.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.booking.Booking;
import com.castilhodigital.facilitei.booking.PaymentStatus;
import com.castilhodigital.facilitei.booking.BookingRepository;
import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.scheduling.Slot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    @Mock
    private BookingRepository bookingRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(bookingRepository);
    }

    private Booking booking(String telefone, String nome, BigDecimal preco, OffsetDateTime dataHora,
                            PaymentStatus status, Boolean compareceu) {
        ServiceOffering service = new ServiceOffering();
        service.setPreco(preco);

        Slot slot = new Slot();
        slot.setService(service);
        slot.setDataHora(dataHora);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteTelefone(telefone);
        booking.setClienteNome(nome);
        booking.setStatusPagamento(status);
        booking.setCompareceu(compareceu);
        return booking;
    }

    @Test
    void gerarRelatorioCalculaFaturamentoContandoCompareceuENaoMarcadosMasNaoNaoComparecimentos() {
        OffsetDateTime dia = LocalDate.now().atStartOfDay(ZONE_ID).toOffsetDateTime();
        List<Booking> bookings = List.of(
                booking("+5511111111111", "Cliente A", new BigDecimal("50.00"), dia, PaymentStatus.PAGO, true),
                booking("+5511222222222", "Cliente B", new BigDecimal("30.00"), dia, PaymentStatus.SEM_SINAL, null),
                booking("+5511333333333", "Cliente C", new BigDecimal("20.00"), dia, PaymentStatus.PAGO, false));
        when(bookingRepository.findParaRelatorio(anyLong(), any(), any(), any())).thenReturn(bookings);

        RelatorioResponse relatorio = reportService.gerarRelatorio(1L, LocalDate.now(), LocalDate.now());

        assertThat(relatorio.faturamentoTotal()).isEqualByComparingTo("80.00");
        assertThat(relatorio.totalReservasConfirmadas()).isEqualTo(3);
    }

    @Test
    void gerarRelatorioCalculaTaxaDeNaoComparecimentoSoSobreOsMarcados() {
        OffsetDateTime dia = LocalDate.now().atStartOfDay(ZONE_ID).toOffsetDateTime();
        List<Booking> bookings = List.of(
                booking("+5511111111111", "Cliente A", BigDecimal.TEN, dia, PaymentStatus.PAGO, true),
                booking("+5511222222222", "Cliente B", BigDecimal.TEN, dia, PaymentStatus.PAGO, false),
                booking("+5511333333333", "Cliente C", BigDecimal.TEN, dia, PaymentStatus.SEM_SINAL, null));
        when(bookingRepository.findParaRelatorio(anyLong(), any(), any(), any())).thenReturn(bookings);

        RelatorioResponse relatorio = reportService.gerarRelatorio(1L, LocalDate.now(), LocalDate.now());

        assertThat(relatorio.totalComparecimentosMarcados()).isEqualTo(2);
        assertThat(relatorio.totalNaoComparecimentos()).isEqualTo(1);
        assertThat(relatorio.taxaNaoComparecimentoPercentual()).isEqualByComparingTo("50.0");
    }

    @Test
    void gerarRelatorioSemNenhumaMarcacaoRetornaTaxaZeroSemDivisaoPorZero() {
        OffsetDateTime dia = LocalDate.now().atStartOfDay(ZONE_ID).toOffsetDateTime();
        List<Booking> bookings = List.of(
                booking("+5511111111111", "Cliente A", BigDecimal.TEN, dia, PaymentStatus.PAGO, null));
        when(bookingRepository.findParaRelatorio(anyLong(), any(), any(), any())).thenReturn(bookings);

        RelatorioResponse relatorio = reportService.gerarRelatorio(1L, LocalDate.now(), LocalDate.now());

        assertThat(relatorio.taxaNaoComparecimentoPercentual()).isEqualByComparingTo("0");
    }

    @Test
    void gerarRelatorioAgrupaClientesRecorrentesPorTelefoneEIgnoraQuemAgendouUmaSoVez() {
        OffsetDateTime primeiraVisita = LocalDate.now().minusDays(10).atStartOfDay(ZONE_ID).toOffsetDateTime();
        OffsetDateTime segundaVisita = LocalDate.now().atStartOfDay(ZONE_ID).toOffsetDateTime();
        List<Booking> bookings = List.of(
                booking("+5511111111111", "Cliente Recorrente", BigDecimal.TEN, primeiraVisita, PaymentStatus.PAGO, true),
                booking("+5511111111111", "Cliente Recorrente", BigDecimal.TEN, segundaVisita, PaymentStatus.SEM_SINAL, null),
                booking("+5511222222222", "Cliente Unico", BigDecimal.TEN, segundaVisita, PaymentStatus.PAGO, true));
        when(bookingRepository.findParaRelatorio(anyLong(), any(), any(), any())).thenReturn(bookings);

        RelatorioResponse relatorio = reportService.gerarRelatorio(1L, LocalDate.now(), LocalDate.now());

        assertThat(relatorio.clientesRecorrentes()).hasSize(1);
        ClienteRecorrenteResponse recorrente = relatorio.clientesRecorrentes().get(0);
        assertThat(recorrente.clienteTelefone()).isEqualTo("+5511111111111");
        assertThat(recorrente.totalAgendamentos()).isEqualTo(2);
        assertThat(recorrente.ultimoAgendamento()).isEqualTo(segundaVisita);
    }

}
