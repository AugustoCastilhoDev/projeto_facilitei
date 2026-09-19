package com.castilhodigital.facilitei.billing;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.payment.asaas.AsaasProperties;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera a fatura (+ cobranca Pix real) de UM tenant, em transacao propria.
 *
 * Fica num bean separado de AssinaturaService de proposito: o lote diario
 * (AssinaturaService.gerarFaturasDoDia) chama este metodo uma vez por tenant,
 * e uma chamada dentro da mesma classe ignoraria o proxy transacional do
 * Spring. Com uma transacao por tenant, a cobranca rejeitada de um (ex.:
 * CPF/CNPJ invalido na Asaas) desfaz so o que era dele, em vez de derrubar a
 * fatura de todos os outros do mesmo lote.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaturaGeracaoService {

    private static final int PRAZO_VENCIMENTO_DIAS = 3;

    private final TenantRepository tenantRepository;
    private final FaturaRepository faturaRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final AsaasProperties asaasProperties;

    /**
     * Nao faz nada se ja houver uma fatura PENDENTE aberta (evita duplicar a
     * cobranca se o scheduler rodar mais de uma vez no mesmo dia) ou se o
     * tenant nao tiver CPF/CNPJ (a Asaas exige um por customer).
     */
    @Transactional
    public void gerarSeNecessario(Long tenantId, LocalDate hoje) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Tenant nao encontrado (id=" + tenantId + ")."));

        if (faturaRepository.existsByTenantIdAndStatus(tenant.getId(), FaturaStatus.PENDENTE)) {
            return;
        }
        if (tenant.getCpfCnpj() == null || tenant.getCpfCnpj().isBlank()) {
            log.warn("Tenant '{}' sem cpfCnpj cadastrado - pulando geracao de fatura da assinatura.", tenant.getSlug());
            return;
        }

        Fatura fatura = new Fatura();
        fatura.setTenant(tenant);
        fatura.setPlano(tenant.getPlano());
        fatura.setValor(tenant.getPlano().getPrecoMensal());
        fatura.setStatus(FaturaStatus.PENDENTE);
        fatura.setCompetencia(hoje);
        fatura.setVencimento(hoje.plusDays(PRAZO_VENCIMENTO_DIAS));
        fatura = faturaRepository.save(fatura);

        PixChargeResult resultado = paymentGatewayService.criarCobrancaPix(asaasProperties.apiKey(), new PixChargeRequest(
                tenant.getNome(), null, tenant.getCpfCnpj(), fatura.getValor(), "assinatura-" + fatura.getId()));
        fatura.setAsaasPaymentId(resultado.paymentId());

        tenant.setAssinaturaStatus(AssinaturaStatus.INADIMPLENTE);
    }

}
