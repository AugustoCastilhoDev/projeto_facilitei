package com.castilhodigital.facilitei.scheduling;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    boolean existsByProfissionalIdAndServiceIdAndDataHora(Long profissionalId, Long serviceId, OffsetDateTime dataHora);

    Optional<Slot> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * JOIN FETCH em "service": estes slots sao mapeados para SlotResponse
     * (que le service.nome) fora da transacao, na camada de controller.
     * Como open-in-view=false (etapa 1), sem o fetch aqui o acesso a
     * slot.getService().getNome() estoura LazyInitializationException -
     * foi exatamente o que aconteceu no teste manual desta etapa.
     *
     * Filtra tambem por service.ativo = true: um servico desativado nao
     * deve mais ser reservavel publicamente, mesmo que ainda existam slots
     * DISPONIVEL gerados antes da desativacao (bug encontrado em teste
     * manual - a geracao de horarios ja bloqueia servico inativo, mas sem
     * este filtro slots antigos continuariam aparecendo na agenda publica).
     */
    @Query("SELECT s FROM Slot s JOIN FETCH s.service JOIN FETCH s.profissional "
            + "WHERE s.tenant.id = :tenantId AND s.status = :status AND s.service.ativo = true "
            + "AND s.dataHora BETWEEN :inicio AND :fim ORDER BY s.dataHora")
    List<Slot> findDisponiveisComServico(@Param("tenantId") Long tenantId, @Param("status") SlotStatus status,
                                          @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

    /**
     * Agenda completa do admin (todos os status), diferente da listagem
     * publica que so mostra DISPONIVEL. profissionalId e opcional (null =
     * todos os profissionais do tenant).
     */
    @Query("SELECT s FROM Slot s JOIN FETCH s.service JOIN FETCH s.profissional "
            + "WHERE s.tenant.id = :tenantId AND (:profissionalId IS NULL OR s.profissional.id = :profissionalId) "
            + "AND s.dataHora BETWEEN :inicio AND :fim ORDER BY s.dataHora")
    List<Slot> findAgendaComServico(@Param("tenantId") Long tenantId, @Param("profissionalId") Long profissionalId,
                                     @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

    /**
     * Slots ocupados (RESERVADO/CONFIRMADO) de um profissional especifico no
     * intervalo informado - usado para detectar conflito de horario entre
     * servicos diferentes do MESMO profissional (ex.: "Corte" e "Barba"
     * gerados para o mesmo horario do mesmo profissional). JOIN FETCH em
     * "service" porque o calculo de sobreposicao precisa de duracaoMin.
     */
    @Query("SELECT s FROM Slot s JOIN FETCH s.service "
            + "WHERE s.profissional.id = :profissionalId AND s.status IN :statuses AND s.dataHora BETWEEN :inicio AND :fim")
    List<Slot> findOcupadosNoIntervalo(@Param("profissionalId") Long profissionalId,
                                        @Param("statuses") List<SlotStatus> statuses,
                                        @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

}
