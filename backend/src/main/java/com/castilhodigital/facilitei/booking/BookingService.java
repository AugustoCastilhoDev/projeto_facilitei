package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.notification.NotificationService;
import com.castilhodigital.facilitei.scheduling.Slot;
import com.castilhodigital.facilitei.scheduling.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria e mantem o ciclo de vida da reserva em si (dominio puro). A
 * orquestracao com o gateway de pagamento (Asaas) fica em
 * BookingCheckoutService, que chama criarReserva() e depois vincularCobranca()
 * - esta classe nao sabe nada sobre Pix/Asaas, so sobre slot+booking.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotService slotService;
    private final NotificationService notificationService;

    @Transactional
    public Booking criarReserva(Long slotId, String clienteNome, String clienteTelefone, String clienteCpfCnpj) {
        Slot slot = slotService.reservar(slotId);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setClienteNome(clienteNome);
        booking.setClienteTelefone(clienteTelefone);
        booking.setClienteCpfCnpj(clienteCpfCnpj);
        booking.setStatusPagamento(PaymentStatus.PENDENTE);
        booking = bookingRepository.save(booking);

        notificationService.enviar(clienteTelefone,
                "Ola " + clienteNome + "! Sua reserva foi criada. Finalize o pagamento do sinal via Pix para confirmar.");

        return booking;
    }

    /** Preenche os dados da cobranca Pix apos criar a cobranca no Asaas. */
    @Transactional
    public void vincularCobranca(Long bookingId, String asaasPaymentId, String pixPayload) {
        Booking booking = buscarPorId(bookingId);
        booking.setAsaasPaymentId(asaasPaymentId);
        booking.setAsaasPixPayload(pixPayload);
    }

    /** Chamado pelo endpoint de webhook do Asaas (etapa 6) quando o pagamento e confirmado. */
    @Transactional
    public void confirmarPagamento(String asaasPaymentId) {
        Booking booking = bookingRepository.findByAsaasPaymentId(asaasPaymentId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Nenhuma reserva encontrada para o pagamento Asaas '" + asaasPaymentId + "'."));

        booking.setStatusPagamento(PaymentStatus.PAGO);
        slotService.confirmar(booking.getSlot().getId());

        notificationService.enviar(booking.getClienteTelefone(), "Pagamento confirmado! Seu horario esta garantido.");
    }

    @Transactional(readOnly = true)
    public Booking buscarPorId(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva nao encontrada (id=" + bookingId + ")."));
    }

}
