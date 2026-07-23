package com.castilhodigital.facilitei.billing;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma cobranca da mensalidade da assinatura (plataforma -> tenant) - nao
 * confundir com Booking, que e a cobranca do sinal (cliente final -> tenant,
 * modelo BYOPP). Cada fatura corresponde a uma tentativa de cobranca Pix na
 * conta Asaas da PROPRIA plataforma.
 */
@Entity
@Table(name = "faturas")
@Getter
@Setter
@NoArgsConstructor
public class Fatura extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plano plano;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "asaas_payment_id", length = 60)
    private String asaasPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FaturaStatus status;

    /** Mes de referencia da cobranca (dia 1 do mes, ou a data em que o ciclo comecou). */
    @Column(nullable = false)
    private LocalDate competencia;

    @Column(nullable = false)
    private LocalDate vencimento;

}
