package com.castilhodigital.facilitei.billing;

import java.math.BigDecimal;

/**
 * Planos fixos da assinatura, definidos no codigo (nao editaveis via admin) -
 * mesmo espirito de PaymentStatus. O limite de profissionais e o
 * diferencial funcional entre os planos (ver AssinaturaGuard).
 */
public enum Plano {

    BASICO(new BigDecimal("49.00"), 2),
    PROFISSIONAL(new BigDecimal("89.00"), 5),
    PREMIUM(new BigDecimal("149.00"), Integer.MAX_VALUE);

    private final BigDecimal precoMensal;
    private final int limiteProfissionais;

    Plano(BigDecimal precoMensal, int limiteProfissionais) {
        this.precoMensal = precoMensal;
        this.limiteProfissionais = limiteProfissionais;
    }

    public BigDecimal getPrecoMensal() {
        return precoMensal;
    }

    public int getLimiteProfissionais() {
        return limiteProfissionais;
    }

}
