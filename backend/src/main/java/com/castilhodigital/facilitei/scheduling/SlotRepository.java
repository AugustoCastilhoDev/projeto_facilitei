package com.castilhodigital.facilitei.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    boolean existsByServiceIdAndDataHora(Long serviceId, OffsetDateTime dataHora);

    Optional<Slot> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * JOIN FETCH em "service": estes slots sao mapeados para SlotResponse
     * (que le service.nome) fora da transacao, na camada de controller.
     * Como open-in-view=false (etapa 1), sem o fetch aqui o acesso a
     * slot.getService().getNome() estoura LazyInitializationException -
     * foi exatamente o que aconteceu no teste manual desta etapa.
     */
    @Query("SELECT s FROM Slot s JOIN FETCH s.service "
            + "WHERE s.tenant.id = :tenantId AND s.status = :status AND s.dataHora BETWEEN :inicio AND :fim "
            + "ORDER BY s.dataHora")
    List<Slot> findDisponiveisComServico(@Param("tenantId") Long tenantId, @Param("status") SlotStatus status,
                                          @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

    /** Agenda completa do admin (todos os status), diferente da listagem publica que so mostra DISPONIVEL. */
    @Query("SELECT s FROM Slot s JOIN FETCH s.service "
            + "WHERE s.tenant.id = :tenantId AND s.dataHora BETWEEN :inicio AND :fim "
            + "ORDER BY s.dataHora")
    List<Slot> findAgendaComServico(@Param("tenantId") Long tenantId,
                                     @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

}
