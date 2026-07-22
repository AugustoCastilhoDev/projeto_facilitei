package com.castilhodigital.facilitei.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.scheduling.Slot;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingCheckoutServiceTest {

    private static final String API_KEY_TENANT = "chave-asaas-do-tenant";

    @Mock
    private BookingService bookingService;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    private BookingCheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new BookingCheckoutService(bookingService, paymentGatewayService);
    }

    private Tenant tenantComChaveConfigurada() {
        Tenant tenant = new Tenant();
        tenant.setAsaasApiKey(API_KEY_TENANT);
        return tenant;
    }

    @Test
    void iniciarCheckoutCalculaSinalEVinculaCobranca() {
        ServiceOffering service = new ServiceOffering();
        service.setPreco(new BigDecimal("100.00"));
        service.setSinalPercentual(new BigDecimal("25.00"));

        Slot slot = new Slot();
        slot.setService(service);
        slot.setTenant(tenantComChaveConfigurada());

        Booking booking = new Booking();
        booking.setSlot(slot);
        ReflectionTestUtils.setField(booking, "id", 5L);

        when(bookingService.criarReserva(1L, "Cliente", "+5511999999999", "111.111.111-11")).thenReturn(booking);
        when(paymentGatewayService.criarCobrancaPix(eq(API_KEY_TENANT), any()))
                .thenReturn(new PixChargeResult("pay_1", "payload-x", "base64-y"));
        when(bookingService.buscarPorId(5L)).thenReturn(booking);

        CheckoutResult resultado = checkoutService.iniciarCheckout(1L, "Cliente", "+5511999999999", "111.111.111-11");

        ArgumentCaptor<PixChargeRequest> captor = ArgumentCaptor.forClass(PixChargeRequest.class);
        verify(paymentGatewayService).criarCobrancaPix(eq(API_KEY_TENANT), captor.capture());
        assertThat(captor.getValue().valor()).isEqualByComparingTo("25.00");
        assertThat(captor.getValue().referenciaExterna()).isEqualTo("booking-5");
        assertThat(captor.getValue().clienteCpfCnpj()).isEqualTo("111.111.111-11");

        verify(bookingService).vincularCobranca(5L, "pay_1", "payload-x");
        assertThat(resultado.qrCodeImagemBase64()).isEqualTo("base64-y");
        assertThat(resultado.booking()).isSameAs(booking);
    }

    @Test
    void iniciarCheckoutDeServicoSemSinalConfirmaDiretoSemChamarAsaas() {
        ServiceOffering service = new ServiceOffering();
        service.setPreco(new BigDecimal("100.00"));
        service.setSinalPercentual(BigDecimal.ZERO);

        Slot slot = new Slot();
        slot.setService(service);

        Booking booking = new Booking();
        booking.setSlot(slot);
        ReflectionTestUtils.setField(booking, "id", 5L);

        when(bookingService.criarReserva(1L, "Cliente", "+5511999999999", "111.111.111-11")).thenReturn(booking);
        when(bookingService.buscarPorId(5L)).thenReturn(booking);

        CheckoutResult resultado = checkoutService.iniciarCheckout(1L, "Cliente", "+5511999999999", "111.111.111-11");

        verify(bookingService).confirmarSemSinal(5L);
        verify(paymentGatewayService, never()).criarCobrancaPix(any(), any());
        assertThat(resultado.qrCodeImagemBase64()).isNull();
    }

    @Test
    void iniciarCheckoutDeServicoComSinalSemChaveAsaasConfiguradaLancaExcecao() {
        ServiceOffering service = new ServiceOffering();
        service.setPreco(new BigDecimal("100.00"));
        service.setSinalPercentual(new BigDecimal("25.00"));

        Slot slot = new Slot();
        slot.setService(service);
        slot.setTenant(new Tenant());

        Booking booking = new Booking();
        booking.setSlot(slot);
        ReflectionTestUtils.setField(booking, "id", 5L);

        when(bookingService.criarReserva(1L, "Cliente", "+5511999999999", "111.111.111-11")).thenReturn(booking);

        assertThatThrownBy(() -> checkoutService.iniciarCheckout(1L, "Cliente", "+5511999999999", "111.111.111-11"))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(paymentGatewayService, never()).criarCobrancaPix(any(), any());
    }

    @Test
    void buscarStatusAtualDePagamentoPendenteRebuscaQrCode() {
        Slot slot = new Slot();
        slot.setTenant(tenantComChaveConfigurada());

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setStatusPagamento(PaymentStatus.PENDENTE);
        booking.setAsaasPaymentId("pay_1");
        ReflectionTestUtils.setField(booking, "id", 5L);

        when(bookingService.buscarPorIdETenantSlug(5L, "barbearia-do-ze")).thenReturn(booking);
        when(paymentGatewayService.buscarQrCodePix(API_KEY_TENANT, "pay_1"))
                .thenReturn(new PixChargeResult("pay_1", "payload-x", "base64-novo"));

        CheckoutResult resultado = checkoutService.buscarStatusAtual(5L, "barbearia-do-ze");

        assertThat(resultado.qrCodeImagemBase64()).isEqualTo("base64-novo");
    }

    @Test
    void buscarStatusAtualDePagamentoJaConfirmadoNaoRebuscaQrCode() {
        Booking booking = new Booking();
        booking.setStatusPagamento(PaymentStatus.PAGO);
        booking.setAsaasPaymentId("pay_1");
        ReflectionTestUtils.setField(booking, "id", 5L);

        when(bookingService.buscarPorIdETenantSlug(5L, "barbearia-do-ze")).thenReturn(booking);

        CheckoutResult resultado = checkoutService.buscarStatusAtual(5L, "barbearia-do-ze");

        assertThat(resultado.qrCodeImagemBase64()).isNull();
        verify(paymentGatewayService, never()).buscarQrCodePix(any(), any());
    }

}
