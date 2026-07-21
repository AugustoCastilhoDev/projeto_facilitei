package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o checkout completo: reserva o slot, calcula o valor do sinal
 * (preco do servico x sinalPercentual) e gera a cobranca Pix no gateway de
 * pagamento. Fica fora do BookingService para nao acoplar o dominio de
 * agendamento a um provedor de pagamento especifico.
 */
@Service
@RequiredArgsConstructor
public class BookingCheckoutService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final BookingService bookingService;
    private final PaymentGatewayService paymentGatewayService;

    @Transactional
    public CheckoutResult iniciarCheckout(Long slotId, String clienteNome, String clienteTelefone, String clienteCpfCnpj) {
        Booking booking = bookingService.criarReserva(slotId, clienteNome, clienteTelefone, clienteCpfCnpj);

        ServiceOffering service = booking.getSlot().getService();
        BigDecimal valorSinal = service.getPreco()
                .multiply(service.getSinalPercentual())
                .divide(CEM, 2, RoundingMode.HALF_UP);

        PixChargeRequest request = new PixChargeRequest(
                clienteNome, clienteTelefone, clienteCpfCnpj, valorSinal, "booking-" + booking.getId());
        PixChargeResult resultado = paymentGatewayService.criarCobrancaPix(request);

        bookingService.vincularCobranca(booking.getId(), resultado.paymentId(), resultado.payload());

        Booking bookingAtualizado = bookingService.buscarPorId(booking.getId());
        return new CheckoutResult(bookingAtualizado, resultado.qrCodeImagemBase64());
    }

}
