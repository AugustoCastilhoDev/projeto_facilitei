package com.castilhodigital.facilitei.agenda;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    /** Expediente de varios profissionais num dia da semana, numa unica consulta (evita 1 query por profissional). */
    List<HorarioTrabalho> findByProfissionalIdInAndDiaSemanaOrderByInicio(
            Collection<Long> profissionalIds, DayOfWeek diaSemana);

}
