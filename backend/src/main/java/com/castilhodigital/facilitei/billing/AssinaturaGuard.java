package com.castilhodigital.facilitei.billing;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.tenant.Tenant;
import org.springframework.stereotype.Component;

/**
 * Bloqueio "leve" de assinatura: so trava acoes que aumentam o uso (gerar
 * horarios, criar profissional) - login, relatorios e o pagamento da propria
 * fatura continuam liberados mesmo com a assinatura pendente/cancelada.
 */
@Component
public class AssinaturaGuard {

    public void verificarUsoLiberado(Tenant tenant) {
        AssinaturaStatus status = tenant.getAssinaturaStatus();
        if (status == AssinaturaStatus.INADIMPLENTE || status == AssinaturaStatus.CANCELADA) {
            throw new RegraDeNegocioException(
                    "Assinatura " + (status == AssinaturaStatus.CANCELADA ? "cancelada" : "com pagamento pendente")
                            + " - regularize na tela Assinatura para continuar.");
        }
    }

    public void verificarLimiteProfissionais(Tenant tenant, int quantidadeAtual) {
        if (quantidadeAtual >= tenant.getPlano().getLimiteProfissionais()) {
            throw new RegraDeNegocioException(
                    "Limite de profissionais do plano " + tenant.getPlano() + " atingido - faca upgrade de plano para adicionar mais.");
        }
    }

}
