package com.castilhodigital.facilitei.agenda;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

}
