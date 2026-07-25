package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
        return processarCheckout(booking, clienteNome, clienteTelefone, clienteCpfCnpj);
    }

    /**
     * Overload usado pelo portal do cliente: o telefone nunca vem do corpo da
     * requisicao (so do Cliente autenticado, ver PortalBookingController),
     * para nao confiar em telefone informado livremente por quem chama a API.
     */
    @Transactional
    public CheckoutResult iniciarCheckout(Long slotId, Cliente cliente, String clienteNome, String clienteCpfCnpj) {
        Booking booking = bookingService.criarReserva(slotId, cliente, clienteNome, cliente.getTelefone(), clienteCpfCnpj);
        return processarCheckout(booking, clienteNome, cliente.getTelefone(), clienteCpfCnpj);
    }

    private CheckoutResult processarCheckout(Booking booking, String clienteNome, String clienteTelefone, String clienteCpfCnpj) {
        ServiceOffering service = booking.getSlot().getService();
        // Hibernate.initialize forca o carregamento do proxy aqui (mesmo no
        // ramo sem sinal, que nao usa apiKey) porque PortalReservaResponse le
        // slot.getTenant() fora desta transacao (open-in-view: false) - so
        // atribuir a variavel NAO inicializa o proxy, so uma chamada de
        // metodo real faz isso (LazyInitializationException encontrada
        // testando o fluxo completo via Playwright).
        Tenant tenant = booking.getSlot().getTenant();
        Hibernate.initialize(tenant);
        BigDecimal valorSinal = service.getPreco()
                .multiply(service.getSinalPercentual())
                .divide(CEM, 2, RoundingMode.HALF_UP);

        if (valorSinal.signum() <= 0) {
            // servico sem sinal (sinalPercentual = 0): a Asaas nao aceita
            // cobranca de valor zero, entao confirma direto - pagamento no local.
            bookingService.confirmarSemSinal(booking.getId());
            Booking bookingConfirmado = bookingService.buscarPorId(booking.getId());
            return new CheckoutResult(bookingConfirmado, null);
        }

        String apiKey = tenant.getAsaasApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            // modelo BYOPP: a cobranca sai da conta Asaas do proprio tenant,
            // entao sem a chave dele configurada nao ha como gerar o Pix.
            throw new RegraDeNegocioException(
                    "Este negocio ainda nao configurou o recebimento de pagamentos. Tente novamente mais tarde.");
        }

        PixChargeRequest request = new PixChargeRequest(
                clienteNome, clienteTelefone, clienteCpfCnpj, valorSinal, "booking-" + booking.getId());
        PixChargeResult resultado = paymentGatewayService.criarCobrancaPix(apiKey, request);

        bookingService.vincularCobranca(booking.getId(), resultado.paymentId(), resultado.payload());

        Booking bookingAtualizado = bookingService.buscarPorId(booking.getId());
        return new CheckoutResult(bookingAtualizado, resultado.qrCodeImagemBase64());
    }

    /**
     * Consulta o status atual da reserva para a pagina publica de pagamento.
     * Enquanto o pagamento estiver PENDENTE, rebusca o QR Code no Asaas (ele
     * nao e persistido no Booking - so o payload "copia e cola" e guardado),
     * permitindo que a tela de pagamento sobreviva a um F5 do cliente. Uma
     * vez PAGO/EXPIRADO/CANCELADO, nao ha mais motivo para mostrar QR Code,
     * entao evitamos a chamada extra ao gateway.
     */
    @Transactional(readOnly = true)
    public CheckoutResult buscarStatusAtual(Long bookingId, String tenantSlug) {
        Booking booking = bookingService.buscarPorIdETenantSlug(bookingId, tenantSlug);

        if (booking.getStatusPagamento() != PaymentStatus.PENDENTE || booking.getAsaasPaymentId() == null) {
            return new CheckoutResult(booking, null);
        }

        String apiKey = booking.getSlot().getTenant().getAsaasApiKey();
        PixChargeResult qrCode = paymentGatewayService.buscarQrCodePix(apiKey, booking.getAsaasPaymentId());
        return new CheckoutResult(booking, qrCode.qrCodeImagemBase64());
    }

}
