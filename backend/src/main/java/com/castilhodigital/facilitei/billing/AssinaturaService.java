package com.castilhodigital.facilitei.billing;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.payment.asaas.AsaasProperties;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantRepository;
import io.sentry.Sentry;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/**
 * Cobranca da assinatura mensal do PROPRIO tenant com a plataforma - usa a
 * chave Asaas da propria plataforma (facilitei.asaas.api-key), diferente do
 * fluxo de sinal do cliente final (BYOPP, chave por tenant). Reaproveita o
 * mesmo PaymentGatewayService, so trocando de qual conta Asaas e a cobranca.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    private final TenantRepository tenantRepository;
    private final FaturaRepository faturaRepository;
    private final FaturaGeracaoService faturaGeracaoService;
    private final PaymentGatewayService paymentGatewayService;
    private final AsaasProperties asaasProperties;

    /**
     * Gera a fatura (+ cobranca Pix real) para tenants cujo trial acabou de
     * vencer ou cujo ciclo mensal chegou.
     *
     * Deliberadamente SEM @Transactional: cada tenant e faturado na sua
     * propria transacao (FaturaGeracaoService), e uma falha de um (gateway
     * rejeitando o CPF/CNPJ, indisponibilidade momentanea) e registrada e
     * pulada - nao pode impedir a cobranca dos demais nem deixar cobrancas
     * orfas na Asaas por causa de um rollback do lote inteiro. O tenant que
     * falhou continua elegivel e e tentado de novo na proxima execucao.
     */
    public void gerarFaturasDoDia() {
        LocalDate hoje = LocalDate.now(ZONE_ID);

        List<Tenant> elegiveis = new ArrayList<>(
                tenantRepository.findByAssinaturaStatusAndTrialAteLessThan(AssinaturaStatus.TRIAL, hoje));
        elegiveis.addAll(tenantRepository
                .findByAssinaturaStatusAndProximaCobrancaEmIsNotNullAndProximaCobrancaEmLessThanEqual(AssinaturaStatus.ATIVA, hoje));

        for (Tenant tenant : elegiveis) {
            try {
                faturaGeracaoService.gerarSeNecessario(tenant.getId(), hoje);
            } catch (RuntimeException ex) {
                log.error("Falha ao gerar a fatura da assinatura do tenant '{}' - segue para os demais.", tenant.getSlug(), ex);
                Sentry.captureException(ex);
            }
        }
    }

    /** Marca como VENCIDA qualquer fatura PENDENTE cujo prazo passou (o tenant ja ficou INADIMPLENTE ao gera-la). */
    @Transactional
    public void marcarFaturasVencidas() {
        LocalDate hoje = LocalDate.now(ZONE_ID);
        for (Fatura fatura : faturaRepository.findByStatusAndVencimentoLessThan(FaturaStatus.PENDENTE, hoje)) {
            fatura.setStatus(FaturaStatus.VENCIDA);
        }
    }

    @Transactional
    public void confirmarPagamento(String asaasPaymentId) {
        Fatura fatura = buscarFaturaPeloPaymentId(asaasPaymentId);
        fatura.setStatus(FaturaStatus.PAGA);

        Tenant tenant = fatura.getTenant();
        tenant.setAssinaturaStatus(AssinaturaStatus.ATIVA);
        tenant.setProximaCobrancaEm(fatura.getCompetencia().plusMonths(1));
    }

    @Transactional
    public void marcarComoVencida(String asaasPaymentId) {
        buscarFaturaPeloPaymentId(asaasPaymentId).setStatus(FaturaStatus.VENCIDA);
    }

    @Transactional
    public void cancelar(Long tenantId) {
        Tenant tenant = buscarTenant(tenantId);
        tenant.setAssinaturaStatus(AssinaturaStatus.CANCELADA);
    }

    @Transactional(readOnly = true)
    public Tenant buscarTenantPeloAsaasPaymentId(String asaasPaymentId) {
        return buscarFaturaPeloPaymentId(asaasPaymentId).getTenant();
    }

    @Transactional(readOnly = true)
    public AssinaturaResponse consultarStatus(Long tenantId) {
        Tenant tenant = buscarTenant(tenantId);

        FaturaResponse faturaPendente = faturaRepository
                .findTopByTenantIdOrderByCompetenciaDesc(tenantId)
                .filter(f -> f.getStatus() == FaturaStatus.PENDENTE)
                .map(this::toFaturaResponse)
                .orElse(null);

        return new AssinaturaResponse(
                tenant.getPlano(), tenant.getAssinaturaStatus(), tenant.getTrialAte(), tenant.getProximaCobrancaEm(), faturaPendente);
    }

    private FaturaResponse toFaturaResponse(Fatura fatura) {
        PixChargeResult qrCode = paymentGatewayService.buscarQrCodePix(asaasProperties.apiKey(), fatura.getAsaasPaymentId());
        return new FaturaResponse(fatura.getValor(), fatura.getVencimento(), qrCode.payload(), qrCode.qrCodeImagemBase64());
    }

    private Fatura buscarFaturaPeloPaymentId(String asaasPaymentId) {
        return faturaRepository.findByAsaasPaymentId(asaasPaymentId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Fatura nao encontrada para o pagamento Asaas '" + asaasPaymentId + "'."));
    }

    private Tenant buscarTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Tenant nao encontrado (id=" + tenantId + ")."));
    }

}
