package com.castilhodigital.facilitei.billing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaturaRepository extends JpaRepository<Fatura, Long> {

    Optional<Fatura> findByAsaasPaymentId(String asaasPaymentId);

    boolean existsByTenantIdAndStatus(Long tenantId, FaturaStatus status);

    List<Fatura> findByStatusAndVencimentoLessThan(FaturaStatus status, LocalDate data);

    Optional<Fatura> findTopByTenantIdOrderByCompetenciaDesc(Long tenantId);

}
