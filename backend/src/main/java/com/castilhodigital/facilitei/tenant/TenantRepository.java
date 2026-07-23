package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.billing.AssinaturaStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Tenants cujo trial acabou e ainda nao tiveram a 1a fatura gerada (ver AssinaturaService.gerarFaturasDoDia). */
    List<Tenant> findByAssinaturaStatusAndTrialAteLessThan(AssinaturaStatus status, LocalDate data);

    /** Tenants ativos cujo ciclo mensal venceu - so considera datas preenchidas, tenants grandfathered tem proximaCobrancaEm nulo. */
    List<Tenant> findByAssinaturaStatusAndProximaCobrancaEmIsNotNullAndProximaCobrancaEmLessThanEqual(
            AssinaturaStatus status, LocalDate data);

}
