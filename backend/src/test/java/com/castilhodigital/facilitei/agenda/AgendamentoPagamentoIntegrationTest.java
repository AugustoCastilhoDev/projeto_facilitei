package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Pagamento do sinal via gateway (webhook) contra o Postgres real, incluindo o caso que a agenda antiga
 * tratava mal: o pagamento que chega DEPOIS de o horario ter sido liberado.
 */
@ExtendWith(OutputCaptureExtension.class)
class AgendamentoPagamentoIntegrationTest extends CenarioBarbeariaIntegrationTest {

    private static final String COBRANCA = "pay_1";
    private static final Instant DEPOIS_DO_PRAZO = AGORA.plus(Duration.ofMinutes(16));

    @Autowired
    private AgendamentoCheckoutService checkoutService;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoPagamentoService pagamentoService;

    private AgendamentoView checkoutDoCorteAsDez() {
        return checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA).agendamento();
    }

    private AgendamentoStatus statusDe(Long agendamentoId) {
        return agendamentoRepository.findById(agendamentoId).orElseThrow().getStatus();
    }

    @Test
    void pagamentoConfirmaOAgendamentoEmEsperaEAvisaOClienteUmaSoVez() {
        AgendamentoView esperando = checkoutDoCorteAsDez();

        pagamentoService.processarPagamentoRecebido(COBRANCA, AGORA.plusSeconds(60));
        pagamentoService.processarPagamentoRecebido(COBRANCA, AGORA.plusSeconds(61)); // a Asaas reenvia eventos

        assertThat(statusDe(esperando.id())).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(agendamentoRepository.findById(esperando.id()).orElseThrow().getSinalConfirmadoEm())
                .isEqualTo(AGORA.plusSeconds(60));
        verify(notificationService, times(1)).enviar(eq(TELEFONE), contains("está confirmado"));
    }

    @Test
    void pagamentoTardioReativaOAgendamentoSeOHorarioAindaEstaLivre() {
        AgendamentoView esperando = checkoutDoCorteAsDez();
        agendamentoService.expirarVencidos(DEPOIS_DO_PRAZO);
        assertThat(statusDe(esperando.id())).isEqualTo(AgendamentoStatus.EXPIRADO);

        pagamentoService.processarPagamentoRecebido(COBRANCA, DEPOIS_DO_PRAZO.plusSeconds(30));

        assertThat(statusDe(esperando.id())).isEqualTo(AgendamentoStatus.CONFIRMADO);
        verify(notificationService).enviar(eq(TELEFONE), contains("está confirmado"));
    }

    /** O horario foi retomado por outra pessoa: o pagamento tardio NAO pode derrubar nem duplicar a reserva dela. */
    @Test
    void pagamentoTardioComOHorarioJaOcupadoNaoMexeNaReservaDeQuemFicouComEle(CapturedOutput saida) {
        AgendamentoView esperando = checkoutDoCorteAsDez();
        agendamentoService.expirarVencidos(DEPOIS_DO_PRAZO);
        AgendamentoView outraPessoa = agendamentoService.criar(
                pedidoComOutroTelefone(sp(10, 0)), DEPOIS_DO_PRAZO);

        assertThatCode(() -> pagamentoService.processarPagamentoRecebido(COBRANCA, DEPOIS_DO_PRAZO.plusSeconds(30)))
                .doesNotThrowAnyException();

        assertThat(statusDe(esperando.id())).as("o pagamento tardio nao ressuscita quem perdeu o horario")
                .isEqualTo(AgendamentoStatus.EXPIRADO);
        assertThat(statusDe(outraPessoa.id())).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(agendamentoRepository.count()).isEqualTo(2);
        assertThat(saida.getOut()).as("dinheiro entrou por um horario perdido: alguem precisa devolver")
                .contains("devolver o valor ao cliente").contains(COBRANCA);
    }

    @Test
    void pagamentoParaAgendamentoCanceladoNaoMudaNadaMasAlerta(CapturedOutput saida) {
        AgendamentoView esperando = checkoutDoCorteAsDez();
        agendamentoService.cancelarPeloNegocio(negocio.getId(), esperando.id());

        pagamentoService.processarPagamentoRecebido(COBRANCA, AGORA.plusSeconds(60));

        assertThat(statusDe(esperando.id())).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
        assertThat(saida.getOut()).contains("CANCELADO").contains("devolver o valor ao cliente");
    }

    // ---- aviso ao dono: o dinheiro entrou por um horario que nao existe mais ----

    private static final String WHATSAPP_DO_DONO = "5563977776666";

    private void cadastrarWhatsAppDoDono() {
        negocio.setTelefoneDono(WHATSAPP_DO_DONO);
        negocio = tenantRepository.saveAndFlush(negocio);
    }

    /** Antes so havia log/Sentry - que o dono nao le. Ele e quem devolve o sinal, entao precisa saber pelo WhatsApp. */
    @Test
    void pagamentoTardioComHorarioOcupadoAvisaODonoNoWhatsAppComOClienteParaDevolver() {
        cadastrarWhatsAppDoDono();
        checkoutDoCorteAsDez();
        agendamentoService.expirarVencidos(DEPOIS_DO_PRAZO);
        agendamentoService.criar(pedidoComOutroTelefone(sp(10, 0)), DEPOIS_DO_PRAZO);

        pagamentoService.processarPagamentoRecebido(COBRANCA, DEPOIS_DO_PRAZO.plusSeconds(30));

        verify(notificationService).enviar(eq(WHATSAPP_DO_DONO), contains(
                "Atenção: João, fone (63) 99999-9999, pagou o sinal de R$ 15,00 do horário de 21/09 às 10:00, "
                        + "mas o prazo já tinha acabado e o horário foi ocupado por outra pessoa. Devolva o valor ao cliente."));
    }

    @Test
    void pagamentoDeAgendamentoCanceladoTambemAvisaODono() {
        cadastrarWhatsAppDoDono();
        AgendamentoView esperando = checkoutDoCorteAsDez();
        agendamentoService.cancelarPeloNegocio(negocio.getId(), esperando.id());

        pagamentoService.processarPagamentoRecebido(COBRANCA, AGORA.plusSeconds(60));

        verify(notificationService).enviar(eq(WHATSAPP_DO_DONO), contains("mas esse agendamento já estava cancelado"));
    }

    @Test
    void pagamentoTardioQueAindaCabeNaAgendaNaoIncomodaODono() {
        cadastrarWhatsAppDoDono();
        checkoutDoCorteAsDez();
        agendamentoService.expirarVencidos(DEPOIS_DO_PRAZO);

        pagamentoService.processarPagamentoRecebido(COBRANCA, DEPOIS_DO_PRAZO.plusSeconds(30));

        verify(notificationService, never()).enviar(eq(WHATSAPP_DO_DONO), anyString());
    }

    /** Sem WhatsApp cadastrado o aviso nao tem para onde ir, mas o log/Sentry seguem e nada quebra. */
    @Test
    void semWhatsAppDoDonoOAlertaSegueNoLogENaoQuebra(CapturedOutput saida) {
        AgendamentoView esperando = checkoutDoCorteAsDez();
        agendamentoService.cancelarPeloNegocio(negocio.getId(), esperando.id());

        assertThatCode(() -> pagamentoService.processarPagamentoRecebido(COBRANCA, AGORA.plusSeconds(60)))
                .doesNotThrowAnyException();

        assertThat(saida.getOut()).contains("devolver o valor ao cliente").contains("nao tem WhatsApp do dono cadastrado");
    }

    @Test
    void falhaNoWhatsAppDoDonoNaoImpedeOTratamentoDoPagamento() {
        cadastrarWhatsAppDoDono();
        AgendamentoView esperando = checkoutDoCorteAsDez();
        agendamentoService.cancelarPeloNegocio(negocio.getId(), esperando.id());
        doThrow(new RuntimeException("MyZap fora do ar")).when(notificationService).enviar(eq(WHATSAPP_DO_DONO), anyString());

        assertThatCode(() -> pagamentoService.processarPagamentoRecebido(COBRANCA, AGORA.plusSeconds(60)))
                .doesNotThrowAnyException();
    }

    @Test
    void pagamentoDeCobrancaQueNaoEDeNenhumAgendamentoEIgnorado() {
        assertThatCode(() -> pagamentoService.processarPagamentoRecebido("pay_de_outra_coisa", AGORA)).doesNotThrowAnyException();

        assertThat(agendamentoRepository.count()).isZero();
    }

    /** O webhook le o token do negocio fora da transacao: sem JOIN FETCH isso e LazyInitializationException. */
    @Test
    void tenantDaCobrancaChegaComOTokenDoWebhookJaCarregado() {
        negocio.setAsaasWebhookToken("token-do-webhook");
        tenantRepository.saveAndFlush(negocio);
        checkoutDoCorteAsDez();

        assertThat(pagamentoService.buscarTenantDaCobranca(COBRANCA)).get()
                .satisfies(tenant -> assertThat(tenant.getAsaasWebhookToken()).isEqualTo("token-do-webhook"));
        assertThat(pagamentoService.buscarTenantDaCobranca("pay_desconhecido")).isEmpty();
    }

    // ---- cancelamento da cobranca no gateway ----

    @Test
    void expirarCancelaACobrancaPendenteNoGateway() {
        checkoutDoCorteAsDez();

        agendamentoService.expirarVencidos(DEPOIS_DO_PRAZO);

        verify(paymentGatewayService).cancelarCobranca("chave-asaas", COBRANCA);
    }

    @Test
    void cancelarPeloNegocioCancelaACobrancaSeOSinalAindaNaoFoiPago() {
        AgendamentoView esperando = checkoutDoCorteAsDez();

        agendamentoService.cancelarPeloNegocio(negocio.getId(), esperando.id());

        verify(paymentGatewayService).cancelarCobranca("chave-asaas", COBRANCA);
    }

    @Test
    void naoTentaCancelarCobrancaJaPagaQuandoODonoCancelaUmAgendamentoConfirmado() {
        AgendamentoView esperando = checkoutDoCorteAsDez();
        pagamentoService.processarPagamentoRecebido(COBRANCA, AGORA.plusSeconds(60));

        agendamentoService.cancelarPeloNegocio(negocio.getId(), esperando.id());

        verify(paymentGatewayService, never()).cancelarCobranca(eq("chave-asaas"), eq(COBRANCA));
    }

    @Test
    void falhaAoCancelarACobrancaNaoDesfazAExpiracao() {
        AgendamentoView esperando = checkoutDoCorteAsDez();
        doThrow(new PaymentGatewayException("Asaas fora do ar", null))
                .when(paymentGatewayService).cancelarCobranca("chave-asaas", COBRANCA);

        int expirados = agendamentoService.expirarVencidos(DEPOIS_DO_PRAZO);

        assertThat(expirados).isEqualTo(1);
        assertThat(statusDe(esperando.id())).isEqualTo(AgendamentoStatus.EXPIRADO);
    }

    private NovoAgendamento pedidoComOutroTelefone(Instant inicio) {
        return new NovoAgendamento(negocio.getId(), List.of(lavagem.getId()), ana.getId(), inicio,
                "Pedro", "(63) 98888-1111", CPF_VALIDO, null, AgendamentoOrigem.PUBLICO);
    }

}
