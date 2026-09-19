package com.castilhodigital.facilitei.agenda;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

/**
 * Cenario comum dos testes de ciclo de vida do agendamento (segunda 2026-09-21; "agora" = domingo anterior 12:00):
 * Ana faz Corte, Barba e Lavagem (09-12 e 14-18); Bia faz Corte e Lavagem (10-16). Corte e Barba cobram 30% de
 * sinal; Lavagem nao cobra. O negocio recebe o sinal pela Asaas (chave configurada) e o gateway devolve
 * a cobranca "pay_1".
 */
abstract class CenarioBarbeariaIntegrationTest extends AgendaIntegrationTest {

    protected static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    protected static final LocalDate SEGUNDA = LocalDate.of(2026, 9, 21);
    protected static final Instant AGORA = LocalDate.of(2026, 9, 20).atTime(12, 0).atZone(SAO_PAULO).toInstant();
    protected static final String TELEFONE_DIGITADO = "(63) 99999-9999";
    protected static final String TELEFONE = "5563999999999";
    protected static final String CPF_VALIDO = "52998224725";

    protected Tenant negocio;
    protected ServiceOffering corte;
    protected ServiceOffering barba;
    protected ServiceOffering lavagem;
    protected Profissional ana;
    protected Profissional bia;

    @BeforeEach
    void montarCenario() {
        negocio = tenant("barbearia");
        negocio.setAsaasApiKey("chave-asaas");
        negocio = tenantRepository.saveAndFlush(negocio);

        corte = servicoComSinal(negocio, "Corte", 30, "50.00", "30.00");
        barba = servicoComSinal(negocio, "Barba", 30, "35.00", "30.00");
        lavagem = servico(negocio, "Lavagem", 30, "20.00");
        ana = profissional(negocio, "Ana", corte, barba, lavagem);
        bia = profissional(negocio, "Bia", corte, lavagem);

        horario(ana, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        horario(ana, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0));
        horario(bia, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(16, 0));

        when(paymentGatewayService.criarCobrancaPix(anyString(), any(PixChargeRequest.class)))
                .thenReturn(new PixChargeResult("pay_1", "00020126payload", "base64img"));
    }

    protected static Instant sp(int hora, int minuto) {
        return SEGUNDA.atTime(hora, minuto).atZone(SAO_PAULO).toInstant();
    }

    protected NovoAgendamento pedido(List<Long> servicoIds, Long profissionalId, Instant inicio) {
        return new NovoAgendamento(negocio.getId(), servicoIds, profissionalId, inicio,
                "João", TELEFONE_DIGITADO, CPF_VALIDO, null, AgendamentoOrigem.PUBLICO);
    }

    protected NovoAgendamento pedidoDoPainel(List<Long> servicoIds, Long profissionalId, Instant inicio) {
        return new NovoAgendamento(negocio.getId(), servicoIds, profissionalId, inicio,
                "Maria", "63988887777", null, null, AgendamentoOrigem.PAINEL);
    }

}
