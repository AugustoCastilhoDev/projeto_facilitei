package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.professional.Profissional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Periodo em que o profissional nao atende (folga, ferias, consulta medica...), independente do expediente semanal. */
@Entity
@Table(name = "bloqueios")
@Getter
@Setter
@NoArgsConstructor
public class Bloqueio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Column(length = 150)
    private String motivo;

}
