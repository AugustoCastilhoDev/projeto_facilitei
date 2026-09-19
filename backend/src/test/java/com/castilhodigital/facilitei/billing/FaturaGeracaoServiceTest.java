package com.castilhodigital.facilitei.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.payment.asaas.AsaasProperties;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FaturaGeracaoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 19);

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    private FaturaGeracaoService faturaGeracaoService;

    @BeforeEach
    void setUp() {
        AsaasProperties asaasProperties = new AsaasProperties("https://api-sandbox.asaas.com/v3", "chave-plataforma", "webhook-token");
        faturaGeracaoService = new FaturaGeracaoService(tenantRepository, faturaRepository, paymentGatewayService, asaasProperties);
    }

    private Tenant tenant(String cpfCnpj) {
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", 1L);
        tenant.setNome("Barbearia Teste");
        tenant.setAssinaturaStatus(AssinaturaStatus.TRIAL);
        tenant.setPlano(Plano.BASICO);
        tenant.setCpfCnpj(cpfCnpj);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        return tenant;
    }

    @Test
    void geraAFaturaECobrancaPixEMarcaOTenantComoInadimplente() {
        Tenant tenant = tenant("52998224725");
        when(faturaRepository.existsByTenantIdAndStatus(1L, FaturaStatus.PENDENTE)).thenReturn(false);
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> {
            Fatura fatura = invocation.getArgument(0);
            ReflectionTestUtils.setField(fatura, "id", 10L);
            return fatura;
        });
        when(paymentGatewayService.criarCobrancaPix(eq("chave-plataforma"), any(PixChargeRequest.class)))
                .thenReturn(new PixChargeResult("pay_assinatura_1", "payload-pix", "base64img"));

        faturaGeracaoService.gerarSeNecessario(1L, HOJE);

        verify(faturaRepository).save(argThat(fatura -> fatura.getVencimento().equals(HOJE.plusDays(3))));
        verify(paymentGatewayService).criarCobrancaPix(eq("chave-plataforma"),
                argThat(request -> request.valor().compareTo(new BigDecimal("49.00")) == 0
                        && request.referenciaExterna().equals("assinatura-10")));
        assertThat(tenant.getAssinaturaStatus()).isEqualTo(AssinaturaStatus.INADIMPLENTE);
    }

    @Test
    void naoDuplicaSeJaHaFaturaPendente() {
        tenant("52998224725");
        when(faturaRepository.existsByTenantIdAndStatus(1L, FaturaStatus.PENDENTE)).thenReturn(true);

        faturaGeracaoService.gerarSeNecessario(1L, HOJE);

        verify(faturaRepository, never()).save(any());
        verify(paymentGatewayService, never()).criarCobrancaPix(any(), any());
    }

    @Test
    void pulaTenantSemCpfCnpj() {
        Tenant tenant = tenant(null);
        when(faturaRepository.existsByTenantIdAndStatus(1L, FaturaStatus.PENDENTE)).thenReturn(false);

        faturaGeracaoService.gerarSeNecessario(1L, HOJE);

        verify(faturaRepository, never()).save(any());
        assertThat(tenant.getAssinaturaStatus()).isEqualTo(AssinaturaStatus.TRIAL);
    }

}
