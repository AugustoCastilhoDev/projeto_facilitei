package com.castilhodigital.facilitei.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.support.PostgresIntegrationTest;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Faturamento da mensalidade contra um Postgres real. O gateway de pagamento
 * e o unico dublê: o que importa aqui e o comportamento transacional entre
 * varios tenants, que so existe de verdade com banco.
 */
class AssinaturaFaturamentoIntegrationTest extends PostgresIntegrationTest {

    private static final String CPF_CNPJ_REJEITADO_PELO_GATEWAY = "00000000000";
    private static final String CPF_VALIDO = "52998224725";

    @Autowired
    private AssinaturaService assinaturaService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private FaturaRepository faturaRepository;

    @MockitoBean
    private PaymentGatewayService paymentGatewayService;

    @BeforeEach
    void configurarGateway() {
        // a Asaas rejeita a cobranca de um CPF/CNPJ invalido - simulado aqui pelo documento
        when(paymentGatewayService.criarCobrancaPix(anyString(), any(PixChargeRequest.class))).thenAnswer(invocation -> {
            PixChargeRequest request = invocation.getArgument(1);
            if (CPF_CNPJ_REJEITADO_PELO_GATEWAY.equals(request.clienteCpfCnpj())) {
                throw new PaymentGatewayException("CPF/CNPJ invalido", null);
            }
            return new PixChargeResult("pay_" + request.referenciaExterna(), "payload-pix", "base64img");
        });
    }

    private Tenant tenantComTrialVencido(String slug, String cpfCnpj) {
        Tenant tenant = new Tenant();
        tenant.setNome("Barbearia " + slug);
        tenant.setSlug(slug);
        tenant.setCpfCnpj(cpfCnpj);
        tenant.setPlano(Plano.BASICO);
        tenant.setAssinaturaStatus(AssinaturaStatus.TRIAL);
        tenant.setTrialAte(LocalDate.now().minusDays(1));
        return tenantRepository.save(tenant);
    }

    @Test
    void tenantRejeitadoPeloGatewayNaoImpedeAFaturaDosDemais() {
        // o rejeitado e criado primeiro, entao e o primeiro a ser processado
        Tenant rejeitado = tenantComTrialVencido("rejeitado", CPF_CNPJ_REJEITADO_PELO_GATEWAY);
        Tenant saudavel = tenantComTrialVencido("saudavel", CPF_VALIDO);

        assertThatCode(() -> assinaturaService.gerarFaturasDoDia()).doesNotThrowAnyException();

        List<Fatura> faturasDoSaudavel = faturaRepository.findAll().stream()
                .filter(fatura -> fatura.getTenant().getId().equals(saudavel.getId()))
                .toList();
        assertThat(faturasDoSaudavel).hasSize(1);
        assertThat(faturasDoSaudavel.get(0).getStatus()).isEqualTo(FaturaStatus.PENDENTE);
        assertThat(faturasDoSaudavel.get(0).getAsaasPaymentId()).isNotNull();
        assertThat(tenantRepository.findById(saudavel.getId()).orElseThrow().getAssinaturaStatus())
                .isEqualTo(AssinaturaStatus.INADIMPLENTE);

        // o rejeitado nao ganha fatura orfa (sem cobranca) e continua em trial para nova tentativa
        assertThat(faturaRepository.existsByTenantIdAndStatus(rejeitado.getId(), FaturaStatus.PENDENTE)).isFalse();
        assertThat(tenantRepository.findById(rejeitado.getId()).orElseThrow().getAssinaturaStatus())
                .isEqualTo(AssinaturaStatus.TRIAL);
    }

    @Test
    void rodarDuasVezesNoMesmoDiaNaoDuplicaACobranca() {
        tenantComTrialVencido("unico", CPF_VALIDO);

        assinaturaService.gerarFaturasDoDia();
        assinaturaService.gerarFaturasDoDia();

        assertThat(faturaRepository.count()).isEqualTo(1);
        verify(paymentGatewayService, times(1)).criarCobrancaPix(anyString(), any(PixChargeRequest.class));
    }

}
