package com.castilhodigital.facilitei.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.notification.NotificationService;
import com.castilhodigital.facilitei.scheduling.Slot;
import com.castilhodigital.facilitei.scheduling.SlotService;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SlotService slotService;

    @Mock
    private NotificationService notificationService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, slotService, notificationService);
    }

    @Test
    void marcarComoExpiradoLiberaSlotDeReservaPendente() {
        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 9L);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteTelefone("+5511999999999");
        booking.setStatusPagamento(PaymentStatus.PENDENTE);

        when(bookingRepository.findByAsaasPaymentId("pay_1")).thenReturn(Optional.of(booking));

        bookingService.marcarComoExpirado("pay_1");

        assertThat(booking.getStatusPagamento()).isEqualTo(PaymentStatus.EXPIRADO);
        verify(slotService).liberar(9L);
    }

    @Test
    void marcarComoExpiradoDeReservaJaPagaNaoAlteraNadaNemLiberaSlot() {
        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 9L);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setStatusPagamento(PaymentStatus.PAGO);

        when(bookingRepository.findByAsaasPaymentId("pay_1")).thenReturn(Optional.of(booking));

        bookingService.marcarComoExpirado("pay_1");

        assertThat(booking.getStatusPagamento()).isEqualTo(PaymentStatus.PAGO);
        verify(slotService, never()).liberar(9L);
    }

    @Test
    void expirarReservasProximasDoHorarioExpiraTodasAsPendentesEncontradas() {
        Slot slot1 = new Slot();
        ReflectionTestUtils.setField(slot1, "id", 1L);
        Booking booking1 = new Booking();
        booking1.setSlot(slot1);
        booking1.setStatusPagamento(PaymentStatus.PENDENTE);

        Slot slot2 = new Slot();
        ReflectionTestUtils.setField(slot2, "id", 2L);
        Booking booking2 = new Booking();
        booking2.setSlot(slot2);
        booking2.setStatusPagamento(PaymentStatus.PENDENTE);

        OffsetDateTime limite = OffsetDateTime.now().plusHours(1);
        when(bookingRepository.findByStatusPagamentoAndSlot_DataHoraLessThanEqual(PaymentStatus.PENDENTE, limite))
                .thenReturn(List.of(booking1, booking2));

        int quantidade = bookingService.expirarReservasProximasDoHorario(limite);

        assertThat(quantidade).isEqualTo(2);
        assertThat(booking1.getStatusPagamento()).isEqualTo(PaymentStatus.EXPIRADO);
        assertThat(booking2.getStatusPagamento()).isEqualTo(PaymentStatus.EXPIRADO);
        verify(slotService).liberar(1L);
        verify(slotService).liberar(2L);
    }

    @Test
    void expirarReservasProximasDoHorarioSemPendentesNaoLiberaNada() {
        OffsetDateTime limite = OffsetDateTime.now().plusHours(1);
        when(bookingRepository.findByStatusPagamentoAndSlot_DataHoraLessThanEqual(PaymentStatus.PENDENTE, limite))
                .thenReturn(List.of());

        int quantidade = bookingService.expirarReservasProximasDoHorario(limite);

        assertThat(quantidade).isZero();
        verify(slotService, never()).liberar(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void buscarTenantPeloAsaasPaymentIdRetornaTenantDoSlotDaReserva() {
        Tenant tenant = new Tenant();
        tenant.setSlug("barbearia-do-ze");

        Slot slot = new Slot();
        slot.setTenant(tenant);

        Booking booking = new Booking();
        booking.setSlot(slot);

        when(bookingRepository.findByAsaasPaymentIdFetchTenant("pay_1")).thenReturn(Optional.of(booking));

        Tenant resultado = bookingService.buscarTenantPeloAsaasPaymentId("pay_1");

        assertThat(resultado).isSameAs(tenant);
    }

    @Test
    void buscarTenantPeloAsaasPaymentIdDePagamentoDesconhecidoLancaExcecao() {
        when(bookingRepository.findByAsaasPaymentIdFetchTenant("pay_desconhecido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.buscarTenantPeloAsaasPaymentId("pay_desconhecido"))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void confirmarSemSinalMarcaBookingEConfirmaSlot() {
        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 9L);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteTelefone("+5511999999999");
        booking.setStatusPagamento(PaymentStatus.PENDENTE);
        ReflectionTestUtils.setField(booking, "id", 5L);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        bookingService.confirmarSemSinal(5L);

        assertThat(booking.getStatusPagamento()).isEqualTo(PaymentStatus.SEM_SINAL);
        verify(slotService).confirmar(9L);
    }

    @Test
    void buscarPorIdETenantRetornaReservaQuandoPertenceAoTenant() {
        Booking booking = new Booking();
        when(bookingRepository.findByIdAndSlot_Tenant_Id(5L, 1L)).thenReturn(Optional.of(booking));

        assertThat(bookingService.buscarPorIdETenant(5L, 1L)).isSameAs(booking);
    }

    @Test
    void buscarPorIdETenantLancaQuandoReservaNaoPertenceAoTenant() {
        when(bookingRepository.findByIdAndSlot_Tenant_Id(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.buscarPorIdETenant(5L, 1L))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void marcarComparecimentoMarcaQuandoReservaEstaConfirmada() {
        Booking booking = new Booking();
        booking.setStatusPagamento(PaymentStatus.SEM_SINAL);
        when(bookingRepository.findByIdAndSlot_Tenant_Id(5L, 1L)).thenReturn(Optional.of(booking));

        bookingService.marcarComparecimento(5L, 1L, false);

        assertThat(booking.getCompareceu()).isFalse();
    }

    @Test
    void marcarComparecimentoRejeitaQuandoReservaAindaNaoConfirmada() {
        Booking booking = new Booking();
        booking.setStatusPagamento(PaymentStatus.PENDENTE);
        when(bookingRepository.findByIdAndSlot_Tenant_Id(5L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.marcarComparecimento(5L, 1L, true))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void cancelarLiberaSlotEEnviaNotificacao() {
        Slot slot = new Slot();
        ReflectionTestUtils.setField(slot, "id", 9L);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteTelefone("+5511999999999");
        booking.setStatusPagamento(PaymentStatus.PAGO);
        when(bookingRepository.findByIdAndSlot_Tenant_Id(5L, 1L)).thenReturn(Optional.of(booking));

        bookingService.cancelar(5L, 1L);

        assertThat(booking.getStatusPagamento()).isEqualTo(PaymentStatus.CANCELADO);
        verify(slotService).liberar(9L);
        verify(notificationService).enviar("+5511999999999", "Sua reserva foi cancelada pelo estabelecimento.");
    }

    @Test
    void cancelarRejeitaQuandoReservaJaCancelada() {
        Booking booking = new Booking();
        booking.setStatusPagamento(PaymentStatus.CANCELADO);
        when(bookingRepository.findByIdAndSlot_Tenant_Id(5L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelar(5L, 1L)).isInstanceOf(RegraDeNegocioException.class);
        verify(slotService, never()).liberar(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void cancelarRejeitaQuandoReservaJaExpirada() {
        Booking booking = new Booking();
        booking.setStatusPagamento(PaymentStatus.EXPIRADO);
        when(bookingRepository.findByIdAndSlot_Tenant_Id(5L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelar(5L, 1L)).isInstanceOf(RegraDeNegocioException.class);
        verify(slotService, never()).liberar(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void buscarPorSlotIdsRetornaMapaPorIdDoSlot() {
        Slot slot1 = new Slot();
        ReflectionTestUtils.setField(slot1, "id", 1L);
        Booking booking1 = new Booking();
        booking1.setSlot(slot1);

        Slot slot2 = new Slot();
        ReflectionTestUtils.setField(slot2, "id", 2L);
        Booking booking2 = new Booking();
        booking2.setSlot(slot2);

        when(bookingRepository.findBySlotIdIn(List.of(1L, 2L))).thenReturn(List.of(booking1, booking2));

        var resultado = bookingService.buscarPorSlotIds(List.of(1L, 2L));

        assertThat(resultado).containsEntry(1L, booking1).containsEntry(2L, booking2);
    }

}
