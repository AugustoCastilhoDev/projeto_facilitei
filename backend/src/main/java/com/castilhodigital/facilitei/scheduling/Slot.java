package com.castilhodigital.facilitei.scheduling;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um horario concreto e agendavel de um servico. E gerado automaticamente
 * a partir do horario de funcionamento do tenant + duracao do servico
 * (ver etapa 3, geracao de slots).
 */
@Entity
@Table(name = "slots")
@Getter
@Setter
@NoArgsConstructor
public class Slot extends BaseEntity {

    /**
     * Denormalizado a partir de service.tenant para permitir consultas
     * publicas por slug (WHERE tenant_id = ?) sem precisar de join.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status = SlotStatus.DISPONIVEL;

}
