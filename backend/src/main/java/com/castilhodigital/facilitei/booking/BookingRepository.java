package com.castilhodigital.facilitei.booking;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findBySlotId(Long slotId);

    Optional<Booking> findByAsaasPaymentId(String asaasPaymentId);

    /**
     * Mesma busca acima, mas com o tenant do slot ja carregado (JOIN FETCH) -
     * usado pelo webhook do Asaas, que precisa ler Tenant.asaasWebhookToken
     * fora da transacao onde o booking foi buscado (open-in-view: false).
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.slot s JOIN FETCH s.tenant WHERE b.asaasPaymentId = :asaasPaymentId")
    Optional<Booking> findByAsaasPaymentIdFetchTenant(@Param("asaasPaymentId") String asaasPaymentId);

    /** Usado pela pagina publica para consultar o status do pagamento, validando que a reserva e deste tenant. */
    Optional<Booking> findByIdAndSlot_Tenant_Slug(Long id, String slug);

    /** Mesma ideia acima, mas para o lado admin (tenantId em vez de slug) - usado por comparecimento/cancelamento. */
    Optional<Booking> findByIdAndSlot_Tenant_Id(Long id, Long tenantId);

    /** Usado para enriquecer a agenda do admin com dados do cliente sem precisar de associacao bidirecional Slot->Booking. */
    List<Booking> findBySlotIdIn(List<Long> slotIds);

    /**
     * Reservas pendentes cujo horario do slot esta a "limite" de distancia
     * ou ja passou - usado por BookingExpirationScheduler para expirar
     * reservas nao pagas antes do proprio compromisso acontecer (o
     * vencimento por dia da Asaas nao tem essa precisao).
     */
    List<Booking> findByStatusPagamentoAndSlot_DataHoraLessThanEqual(PaymentStatus statusPagamento, OffsetDateTime limite);

    /**
     * Reservas confirmadas (PAGO/SEM_SINAL) do tenant num periodo, usadas
     * pelo relatorio (faturamento, taxa de nao comparecimento, clientes
     * recorrentes). JOIN FETCH em slot+service porque o calculo precisa do
     * preco do servico e da data do slot fora da transacao original.
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.slot s JOIN FETCH s.service "
            + "WHERE s.tenant.id = :tenantId AND s.dataHora BETWEEN :inicio AND :fim AND b.statusPagamento IN :statuses")
    List<Booking> findParaRelatorio(@Param("tenantId") Long tenantId, @Param("statuses") List<PaymentStatus> statuses,
                                     @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

}
