package com.castilhodigital.facilitei.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.asaas.AsaasProperties;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private FaturaGeracaoService faturaGeracaoService;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    private AssinaturaService assinaturaService;

    @BeforeEach
    void setUp() {
        AsaasProperties asaasProperties = new AsaasProperties("https://api-sandbox.asaas.com/v3", "chave-plataforma", "webhook-token");
        assinaturaService = new AssinaturaService(
                tenantRepository, faturaRepository, faturaGeracaoService, paymentGatewayService, asaasProperties);
    }

    private Tenant tenant(Long id, AssinaturaStatus status, Plano plano, String cpfCnpj) {
        Tenant tenant = new Tenant();
        ReflectionTestUtils.setField(tenant, "id", id);
        tenant.setNome("Barbearia Teste");
        tenant.setAssinaturaStatus(status);
        tenant.setPlano(plano);
        tenant.setCpfCnpj(cpfCnpj);
        return tenant;
    }

    @Test
    void gerarFaturasDoDiaDelegaCadaTenantElegivelParaAGeracaoIndividual() {
        Tenant emTrial = tenant(1L, AssinaturaStatus.TRIAL, Plano.BASICO, "52998224725");
        Tenant cicloVencido = tenant(2L, AssinaturaStatus.ATIVA, Plano.PROFISSIONAL, "52998224725");
        when(tenantRepository.findByAssinaturaStatusAndTrialAteLessThan(eq(AssinaturaStatus.TRIAL), any()))
                .thenReturn(List.of(emTrial));
        when(tenantRepository.findByAssinaturaStatusAndProximaCobrancaEmIsNotNullAndProximaCobrancaEmLessThanEqual(
                eq(AssinaturaStatus.ATIVA), any())).thenReturn(List.of(cicloVencido));

        assinaturaService.gerarFaturasDoDia();

        verify(faturaGeracaoService).gerarSeNecessario(eq(1L), any(LocalDate.class));
        verify(faturaGeracaoService).gerarSeNecessario(eq(2L), any(LocalDate.class));
    }

    @Test
    void gerarFaturasDoDiaSegueParaOsDemaisQuandoUmTenantFalha() {
        Tenant quebrado = tenant(1L, AssinaturaStatus.TRIAL, Plano.BASICO, "00000000000");
        Tenant saudavel = tenant(2L, AssinaturaStatus.TRIAL, Plano.BASICO, "52998224725");
        when(tenantRepository.findByAssinaturaStatusAndTrialAteLessThan(eq(AssinaturaStatus.TRIAL), any()))
                .thenReturn(List.of(quebrado, saudavel));
        when(tenantRepository.findByAssinaturaStatusAndProximaCobrancaEmIsNotNullAndProximaCobrancaEmLessThanEqual(
                eq(AssinaturaStatus.ATIVA), any())).thenReturn(List.of());
        doThrow(new PaymentGatewayException("rejeitado", null))
                .when(faturaGeracaoService).gerarSeNecessario(eq(1L), any(LocalDate.class));

        assertThatCode(() -> assinaturaService.gerarFaturasDoDia()).doesNotThrowAnyException();

        verify(faturaGeracaoService).gerarSeNecessario(eq(2L), any(LocalDate.class));
    }

    @Test
    void marcarFaturasVencidasAtualizaStatus() {
        Fatura fatura = new Fatura();
        fatura.setStatus(FaturaStatus.PENDENTE);
        when(faturaRepository.findByStatusAndVencimentoLessThan(eq(FaturaStatus.PENDENTE), any()))
                .thenReturn(List.of(fatura));

        assinaturaService.marcarFaturasVencidas();

        assertThat(fatura.getStatus()).isEqualTo(FaturaStatus.VENCIDA);
    }

    @Test
    void confirmarPagamentoAtivaTenantEAvancaProximaCobranca() {
        Tenant tenant = tenant(1L, AssinaturaStatus.INADIMPLENTE, Plano.BASICO, "52998224725");
        Fatura fatura = new Fatura();
        fatura.setTenant(tenant);
        fatura.setStatus(FaturaStatus.PENDENTE);
        fatura.setCompetencia(LocalDate.of(2026, 7, 1));
        when(faturaRepository.findByAsaasPaymentId("pay_1")).thenReturn(Optional.of(fatura));

        assinaturaService.confirmarPagamento("pay_1");

        assertThat(fatura.getStatus()).isEqualTo(FaturaStatus.PAGA);
        assertThat(tenant.getAssinaturaStatus()).isEqualTo(AssinaturaStatus.ATIVA);
        assertThat(tenant.getProximaCobrancaEm()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void cancelarMarcaAssinaturaComoCancelada() {
        Tenant tenant = tenant(1L, AssinaturaStatus.ATIVA, Plano.BASICO, "52998224725");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        assinaturaService.cancelar(1L);

        assertThat(tenant.getAssinaturaStatus()).isEqualTo(AssinaturaStatus.CANCELADA);
    }

    @Test
    void consultarStatusSemFaturaPendenteNaoBuscaQrCode() {
        Tenant tenant = tenant(1L, AssinaturaStatus.ATIVA, Plano.BASICO, "52998224725");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(faturaRepository.findTopByTenantIdOrderByCompetenciaDesc(1L)).thenReturn(Optional.empty());

        AssinaturaResponse resposta = assinaturaService.consultarStatus(1L);

        assertThat(resposta.faturaPendente()).isNull();
        verify(paymentGatewayService, times(0)).buscarQrCodePix(any(), any());
    }

}
