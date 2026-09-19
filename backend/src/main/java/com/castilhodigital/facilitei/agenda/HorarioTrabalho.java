package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.professional.Profissional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um intervalo do expediente semanal de um profissional. E "hora de parede"
 * no fuso do negocio (Tenant.fusoHorario), nao um instante: "09:00" significa
 * 09:00 la, e so vira instante ao cruzar com uma data. Varios intervalos no
 * mesmo dia representam pausas (09-12 e 14-18 deixam o almoco livre).
 */
@Entity
@Table(name = "horarios_trabalho")
@Getter
@Setter
@NoArgsConstructor
public class HorarioTrabalho extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime inicio;

    @Column(nullable = false)
    private LocalTime fim;

}
