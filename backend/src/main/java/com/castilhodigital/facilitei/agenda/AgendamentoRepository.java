package com.castilhodigital.facilitei.agenda;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /**
     * Agendamentos que ocupam o horario dos profissionais e tocam a janela
     * [inicio, fim) - base do calculo de disponibilidade. Repete o predicado de
     * status da constraint de exclusao do banco (AgendamentoStatus.QUE_OCUPAM_HORARIO);
     * isso tambem deixa o Postgres usar o indice parcial dessa constraint.
     */
    @Query("SELECT a FROM Agendamento a WHERE a.profissional.id IN :profissionalIds "
            + "AND a.status IN :statuses AND a.inicio < :fim AND a.fim > :inicio")
    List<Agendamento> findOcupados(@Param("profissionalIds") Collection<Long> profissionalIds,
                                    @Param("statuses") Collection<AgendamentoStatus> statuses,
                                    @Param("inicio") Instant inicio, @Param("fim") Instant fim);

    /** Sempre filtrado pelo negocio: o id sequencial sozinho nunca basta para achar um agendamento numa acao do painel. */
    Optional<Agendamento> findByIdAndTenantId(Long id, Long tenantId);

    /** Consulta publica: o codigo aleatorio so vale dentro do negocio do link, para nao vazar reserva de outro negocio. */
    Optional<Agendamento> findByCodigoPublicoAndTenantSlug(UUID codigoPublico, String slug);

    Optional<Agendamento> findByAsaasPaymentId(String asaasPaymentId);

    /** Quantas reservas deste telefone estao segurando horario sem pagar - limite contra quem "esgota" a agenda de um negocio. */
    long countByTenantIdAndClienteTelefoneAndStatusIn(Long tenantId, String clienteTelefone,
                                                       Collection<AgendamentoStatus> statuses);

    /**
     * Agendamentos que estavam esperando e ja passaram do prazo, em lote. O
     * lock pessimista com SKIP LOCKED (timeout -2 no Hibernate) faz duas instancias da
     * aplicacao rodando o job ao mesmo tempo dividirem o trabalho em vez de
     * disputarem as mesmas linhas ou processarem duas vezes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT a FROM Agendamento a WHERE a.status IN :statuses AND a.expiraEm <= :agora ORDER BY a.expiraEm")
    List<Agendamento> findVencidosParaExpirar(@Param("statuses") Collection<AgendamentoStatus> statuses,
                                               @Param("agora") Instant agora, Limit limite);

    /**
     * Agenda do painel: tudo que toca a janela, de qualquer status (o painel mostra
     * cancelados e expirados, esmaecidos). Busca profissional e servicos junto para a
     * tela nao disparar uma consulta por linha; DISTINCT porque o join com a colecao repete a linha.
     */
    @Query("SELECT DISTINCT a FROM Agendamento a JOIN FETCH a.profissional LEFT JOIN FETCH a.servicos "
            + "WHERE a.tenant.id = :tenantId AND (:profissionalId IS NULL OR a.profissional.id = :profissionalId) "
            + "AND a.inicio < :fim AND a.fim > :inicio ORDER BY a.inicio")
    List<Agendamento> findAgenda(@Param("tenantId") Long tenantId, @Param("profissionalId") Long profissionalId,
                                  @Param("inicio") Instant inicio, @Param("fim") Instant fim);

}
