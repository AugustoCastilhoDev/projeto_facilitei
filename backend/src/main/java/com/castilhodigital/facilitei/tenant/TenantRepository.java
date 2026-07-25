package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.billing.AssinaturaStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Diretorio publico do portal do cliente: so tenants que optaram por
     * aparecer (visivelNoDiretorio = true), com filtros opcionais (qualquer
     * parametro nulo e ignorado). Sem paginacao de proposito no MVP - o
     * volume de tenants ainda nao justifica a complexidade.
     *
     * cidadeLike/qLike ja chegam prontos ("%valor%", em minusculas) de
     * PublicDirectoryController - LOWER(CONCAT('%', :param, '%')) direto na
     * query falha no Postgres quando o parametro e nulo ("function
     * lower(bytea) does not exist", confirmado testando contra o Postgres
     * real): o driver nao consegue inferir o tipo de um parametro nulo usado
     * dentro de uma funcao. Aplicando LOWER() so na coluna (tipo conhecido) e
     * comparando contra um LIKE ja pronto evita o problema.
     */
    @Query("SELECT t FROM Tenant t WHERE t.visivelNoDiretorio = true "
            + "AND (:categoria IS NULL OR t.categoria = :categoria) "
            + "AND (:cidadeLike IS NULL OR LOWER(t.cidade) LIKE :cidadeLike) "
            + "AND (:qLike IS NULL OR LOWER(t.nome) LIKE :qLike) "
            + "ORDER BY t.nome")
    List<Tenant> buscarNoDiretorio(@Param("categoria") CategoriaNegocio categoria,
                                    @Param("cidadeLike") String cidadeLike, @Param("qLike") String qLike);

    /** Tenants cujo trial acabou e ainda nao tiveram a 1a fatura gerada (ver AssinaturaService.gerarFaturasDoDia). */
    List<Tenant> findByAssinaturaStatusAndTrialAteLessThan(AssinaturaStatus status, LocalDate data);

    /** Tenants ativos cujo ciclo mensal venceu - so considera datas preenchidas, tenants grandfathered tem proximaCobrancaEm nulo. */
    List<Tenant> findByAssinaturaStatusAndProximaCobrancaEmIsNotNullAndProximaCobrancaEmLessThanEqual(
            AssinaturaStatus status, LocalDate data);

}
