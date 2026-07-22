package com.castilhodigital.facilitei.professional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    /**
     * LEFT JOIN FETCH em "servicos": ProfissionalResponse.from() le
     * profissional.getServicos() fora da transacao, na camada de controller.
     * Como open-in-view=false, sem o fetch aqui o acesso a essa colecao
     * @ManyToMany (lazy por padrao) estoura LazyInitializationException -
     * mesmo problema ja documentado para slot.getService() no pacote
     * scheduling, pego aqui em teste manual (nao em nenhum teste automatizado,
     * ja que os mocks nao exercitam o Hibernate de verdade). DISTINCT evita
     * linhas duplicadas do profissional quando ele tem mais de um servico.
     */
    @Query("SELECT DISTINCT p FROM Profissional p LEFT JOIN FETCH p.servicos WHERE p.tenant.id = :tenantId ORDER BY p.nome")
    List<Profissional> findByTenantIdOrderByNome(@Param("tenantId") Long tenantId);

    @Query("SELECT DISTINCT p FROM Profissional p LEFT JOIN FETCH p.servicos "
            + "WHERE p.tenant.id = :tenantId AND p.ativo = true ORDER BY p.nome")
    List<Profissional> findByTenantIdAndAtivoTrueOrderByNome(@Param("tenantId") Long tenantId);

    @Query("SELECT DISTINCT p FROM Profissional p LEFT JOIN FETCH p.servicos WHERE p.id = :id AND p.tenant.id = :tenantId")
    Optional<Profissional> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * Profissionais ativos do tenant que realizam um servico especifico -
     * usado no fluxo publico e na geracao de horarios. O fetch de "servicos"
     * usa um LEFT JOIN separado do filtro (feito via EXISTS) para nao
     * corromper a colecao com apenas o servico filtrado - um JOIN FETCH
     * condicionado ao mesmo predicado do WHERE devolveria a colecao
     * incompleta (so o servico que bateu no filtro, nao todos).
     */
    @Query("SELECT DISTINCT p FROM Profissional p LEFT JOIN FETCH p.servicos "
            + "WHERE p.tenant.id = :tenantId AND p.ativo = true "
            + "AND EXISTS (SELECT 1 FROM Profissional p2 JOIN p2.servicos s2 WHERE p2 = p AND s2.id = :serviceId) "
            + "ORDER BY p.nome")
    List<Profissional> findByTenantIdAndServicosIdAndAtivoTrueOrderByNome(@Param("tenantId") Long tenantId,
                                                                           @Param("serviceId") Long serviceId);

}
