package com.castilhodigital.facilitei.agenda;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    /** Expediente de varios profissionais num dia da semana, numa unica consulta (evita 1 query por profissional). */
    List<HorarioTrabalho> findByProfissionalIdInAndDiaSemanaOrderByInicio(
            Collection<Long> profissionalIds, DayOfWeek diaSemana);

    List<HorarioTrabalho> findByProfissionalIdOrderByDiaSemanaAscInicioAsc(Long profissionalId);

    /** Exclusao em massa (executa na hora, sem passar pela fila de acoes do Hibernate). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM HorarioTrabalho h WHERE h.profissional.id = :profissionalId")
    void excluirTodosDoProfissional(@Param("profissionalId") Long profissionalId);

}
