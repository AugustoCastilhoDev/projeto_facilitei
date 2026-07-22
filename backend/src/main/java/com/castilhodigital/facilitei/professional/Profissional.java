package com.castilhodigital.facilitei.professional;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um profissional/recurso do tenant, com expediente proprio (horario de
 * abertura/fechamento) e um conjunto de servicos que ele realiza. Cada Slot
 * pertence a um unico profissional - dois servicos diferentes so conflitam
 * entre si (ver SlotService) quando sao do mesmo profissional.
 */
@Entity
@Table(name = "profissionais")
@Getter
@Setter
@NoArgsConstructor
public class Profissional extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String nome;

    @Column(name = "horario_abertura", nullable = false)
    private LocalTime horarioAbertura;

    @Column(name = "horario_fechamento", nullable = false)
    private LocalTime horarioFechamento;

    /** Soft-disable: profissionais inativos somem da tela de geracao de horarios e do fluxo publico. */
    @Column(nullable = false)
    private boolean ativo = true;

    /** Servicos que este profissional realiza - define quais combinacoes sao validas ao gerar horarios. */
    @ManyToMany
    @JoinTable(
            name = "profissional_servicos",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceOffering> servicos = new HashSet<>();

}
