package com.castilhodigital.facilitei.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findByTenantIdOrderByNome(Long tenantId);

    List<ServiceOffering> findByTenantIdAndAtivoTrueOrderByNome(Long tenantId);

    Optional<ServiceOffering> findByIdAndTenantId(Long id, Long tenantId);

}
