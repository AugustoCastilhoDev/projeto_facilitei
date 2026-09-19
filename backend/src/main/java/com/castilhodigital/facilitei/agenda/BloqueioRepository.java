package com.castilhodigital.facilitei.agenda;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    /**
     * Bloqueios que tocam a janela [inicio, fim). A condicao e de sobreposicao
     * (nao "comeca dentro da janela"): um bloqueio de ferias que comecou na
     * semana passada e termina na proxima precisa aparecer para o dia de hoje.
     */
    @Query("SELECT b FROM Bloqueio b WHERE b.profissional.id IN :profissionalIds "
            + "AND b.inicio < :fim AND b.fim > :inicio")
    List<Bloqueio> findSobrepostos(@Param("profissionalIds") Collection<Long> profissionalIds,
                                    @Param("inicio") Instant inicio, @Param("fim") Instant fim);

}
