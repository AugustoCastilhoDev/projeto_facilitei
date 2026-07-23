package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.notification.NotificationService;
import com.castilhodigital.facilitei.scheduling.Slot;
import com.castilhodigital.facilitei.scheduling.SlotService;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    /**
     * Usado pelo webhook do Asaas para descobrir de QUAL tenant e o evento
     * recebido, antes de validar o token (cada tenant tem o seu proprio, no
     * modelo BYOPP) - ver AsaasWebhookController.
     */
    @Transactional(readOnly = true)
    public Tenant buscarTenantPeloAsaasPaymentId(String asaasPaymentId) {
        return bookingRepository.findByAsaasPaymentIdFetchTenant(asaasPaymentId)
                .map(booking -> booking.getSlot().getTenant())
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Nenhuma reserva encontrada para o pagamento Asaas '" + asaasPaymentId + "'."));
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

    /**
     * Confirma uma reserva sem cobranca Pix, quando o servico nao exige
     * sinal (sinalPercentual = 0 - ver BookingCheckoutService.iniciarCheckout).
     * O pagamento e combinado para ser feito no local.
     */
    @Transactional
    public void confirmarSemSinal(Long bookingId) {
        Booking booking = buscarPorId(bookingId);
        booking.setStatusPagamento(PaymentStatus.SEM_SINAL);
        slotService.confirmar(booking.getSlot().getId());

        notificationService.enviar(booking.getClienteTelefone(),
                "Sua reserva foi confirmada! O pagamento sera feito no local, sem necessidade de sinal.");
    }

    /**
     * Chamado pelo webhook do Asaas quando a cobranca vence sem pagamento
     * (evento PAYMENT_OVERDUE). Libera o slot de volta para a agenda publica
     * - sem isso o horario ficaria reservado para sempre, ja que nada mais
     * o devolveria (bug encontrado em teste manual desta etapa).
     */
    @Transactional
    public void marcarComoExpirado(String asaasPaymentId) {
        Booking booking = bookingRepository.findByAsaasPaymentId(asaasPaymentId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Nenhuma reserva encontrada para o pagamento Asaas '" + asaasPaymentId + "'."));

        expirarSePendente(booking);
    }

    /**
     * Expira reservas pendentes cujo horario do slot esteja a "limite" de
     * distancia ou ja tenha passado. O vencimento por dia da cobranca Pix
     * na Asaas (ver AsaasPaymentGatewayService) nao tem precisao suficiente
     * para isso - uma reserva de hoje as 14h so venceria por la amanha,
     * bem depois do compromisso. Chamado periodicamente por
     * BookingExpirationScheduler.
     */
    @Transactional
    public int expirarReservasProximasDoHorario(OffsetDateTime limite) {
        List<Booking> pendentesVencendo =
                bookingRepository.findByStatusPagamentoAndSlot_DataHoraLessThanEqual(PaymentStatus.PENDENTE, limite);

        for (Booking booking : pendentesVencendo) {
            expirarSePendente(booking);
        }

        return pendentesVencendo.size();
    }

    /**
     * So age se a reserva ainda estiver PENDENTE: evita reverter uma reserva
     * ja paga caso, por exemplo, o webhook de confirmacao chegue entre a
     * consulta e a expiracao ser efetivada.
     */
    private void expirarSePendente(Booking booking) {
        if (booking.getStatusPagamento() != PaymentStatus.PENDENTE) {
            return;
        }

        booking.setStatusPagamento(PaymentStatus.EXPIRADO);
        slotService.liberar(booking.getSlot().getId());

        notificationService.enviar(booking.getClienteTelefone(),
                "Sua cobranca Pix expirou sem pagamento e o horario foi liberado. Caso ainda queira agendar, faca uma nova reserva.");
    }

    @Transactional(readOnly = true)
    public Booking buscarPorId(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva nao encontrada (id=" + bookingId + ")."));
    }

    /**
     * Usado pela pagina publica de agendamento para consultar (fazer polling
     * de) o status do pagamento apos a criacao da cobranca. Validar o slug
     * evita que um cliente adivinhe o id de uma reserva de outro negocio.
     */
    @Transactional(readOnly = true)
    public Booking buscarPorIdETenantSlug(Long bookingId, String slug) {
        return bookingRepository.findByIdAndSlot_Tenant_Slug(bookingId, slug)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva nao encontrada (id=" + bookingId + ")."));
    }

    /** Usado pelas acoes do admin (comparecimento/cancelamento) - garante que a reserva pertence ao tenant autenticado. */
    @Transactional(readOnly = true)
    public Booking buscarPorIdETenant(Long bookingId, Long tenantId) {
        return bookingRepository.findByIdAndSlot_Tenant_Id(bookingId, tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva nao encontrada (id=" + bookingId + ")."));
    }

    /**
     * Marcacao manual feita pelo admin apos o compromisso, base da taxa de
     * nao comparecimento no relatorio. So faz sentido para reservas ja
     * confirmadas (PAGO/SEM_SINAL) - uma reserva ainda PENDENTE ou ja
     * EXPIRADA/CANCELADA nunca chegou a acontecer de fato.
     */
    @Transactional
    public void marcarComparecimento(Long bookingId, Long tenantId, boolean compareceu) {
        Booking booking = buscarPorIdETenant(bookingId, tenantId);
        if (booking.getStatusPagamento() != PaymentStatus.PAGO && booking.getStatusPagamento() != PaymentStatus.SEM_SINAL) {
            throw new RegraDeNegocioException("So e possivel marcar comparecimento de uma reserva confirmada.");
        }
        booking.setCompareceu(compareceu);
    }

    /** Cancelamento manual pelo admin - libera o slot de volta para a agenda publica e avisa o cliente. */
    @Transactional
    public void cancelar(Long bookingId, Long tenantId) {
        Booking booking = buscarPorIdETenant(bookingId, tenantId);
        if (booking.getStatusPagamento() == PaymentStatus.CANCELADO || booking.getStatusPagamento() == PaymentStatus.EXPIRADO) {
            throw new RegraDeNegocioException("Esta reserva ja nao esta mais ativa.");
        }

        booking.setStatusPagamento(PaymentStatus.CANCELADO);
        slotService.liberar(booking.getSlot().getId());

        notificationService.enviar(booking.getClienteTelefone(), "Sua reserva foi cancelada pelo estabelecimento.");
    }

    /**
     * Usado pela agenda do admin para exibir cliente/status por slot sem
     * precisar de uma associacao bidirecional Slot->Booking. Seguro chamar
     * booking.getSlot().getId() sobre o proxy lazy sem inicializa-lo, ja que
     * o id da FK ja esta disponivel sem precisar de uma query extra.
     */
    @Transactional(readOnly = true)
    public Map<Long, Booking> buscarPorSlotIds(List<Long> slotIds) {
        return bookingRepository.findBySlotIdIn(slotIds).stream()
                .collect(Collectors.toMap(booking -> booking.getSlot().getId(), Function.identity()));
    }

}
