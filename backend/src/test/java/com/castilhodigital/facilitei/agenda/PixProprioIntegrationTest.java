package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sinal recebido na chave Pix do proprio dono (sem Asaas): o negocio do cenario passa a ter so a chave Pix,
 * e o Facilitei monta o "copia e cola" sem chamar gateway nenhum.
 */
class PixProprioIntegrationTest extends CenarioPixProprioIntegrationTest {

    @Autowired
    private AgendamentoCheckoutService checkoutService;

    @Autowired
    private AgendamentoService agendamentoService;

    @Test
    void reservaComSinalGeraOPixDoDonoSemChamarGatewayNenhum() {
        CheckoutAgendamento checkout = checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA);

        AgendamentoView agendamento = checkout.agendamento();
        assertThat(agendamento.status()).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        assertThat(agendamento.meioCobranca()).isEqualTo(MeioCobranca.PIX_PROPRIO);
        assertThat(agendamento.expiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(15)));
        assertThat(checkout.cobranca().payload())
                .contains("br.gov.bcb.pix0114" + CHAVE)
                .contains("540515.00")              // 30% de R$ 50,00
                .contains("FAC" + agendamento.id());
        assertThat(checkout.cobranca().qrCodeBase64()).isNull();
        verifyNoInteractions(paymentGatewayService);
        verify(notificationService).enviar(eq(TELEFONE), contains("Pague o sinal de R$ 15,00 via Pix"));

        var gravado = agendamentoRepository.findById(agendamento.id()).orElseThrow();
        assertThat(gravado.getMeioCobranca()).isEqualTo(MeioCobranca.PIX_PROPRIO);
        assertThat(gravado.getAsaasPaymentId()).isNull();
        assertThat(gravado.getPixPayload()).isEqualTo(checkout.cobranca().payload());
    }

    /** Sem gateway por tras, exigir CPF do cliente seria atrito a toa. */
    @Test
    void naoPedeCpfDoCliente() {
        NovoAgendamento semCpf = new NovoAgendamento(negocio.getId(), List.of(corte.getId()), ana.getId(), sp(10, 0),
                "João", TELEFONE_DIGITADO, null, null, AgendamentoOrigem.PUBLICO);

        assertThat(checkoutService.iniciar(semCpf, AGORA).agendamento().status())
                .isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void consultaDevolveOMesmoPixDoBancoSemNenhumaChamadaExterna() {
        AgendamentoView criado = checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA).agendamento();

        CheckoutAgendamento consulta = checkoutService.consultar("barbearia", criado.codigoPublico());

        assertThat(consulta.cobranca().payload()).isEqualTo(agendamentoRepository.findById(criado.id()).orElseThrow().getPixPayload());
        verifyNoInteractions(paymentGatewayService);
    }

    /** Ativar a Asaas depois nao cria chamada externa para uma reserva que ja nasceu no Pix proprio. */
    @Test
    void ativarAAsaasDepoisNaoMexeNasReservasJaCriadasNoPixProprio() {
        AgendamentoView criado = checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA).agendamento();
        negocio.setAsaasApiKey("chave-asaas-nova");
        tenantRepository.saveAndFlush(negocio);

        checkoutService.consultar("barbearia", criado.codigoPublico());
        agendamentoService.expirarVencidos(AGORA.plus(Duration.ofMinutes(16)));

        assertThat(agendamentoRepository.findById(criado.id()).orElseThrow().getStatus()).isEqualTo(AgendamentoStatus.EXPIRADO);
        verifyNoInteractions(paymentGatewayService);   // nao ha cobranca na Asaas para consultar nem cancelar
    }

    @Test
    void expirarNaoTentaCancelarCobrancaNoGateway() {
        checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA);

        int expirados = agendamentoService.expirarVencidos(AGORA.plus(Duration.ofMinutes(16)));

        assertThat(expirados).isEqualTo(1);
        verifyNoInteractions(paymentGatewayService);
    }

    /** Sem aviso automatico do banco, o dono confirma pelo painel: o mesmo caminho manual ja existente. */
    @Test
    void donoConfirmaOPagamentoEOClienteERecebeOAvisoDeConfirmado() {
        AgendamentoView criado = checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA).agendamento();

        AgendamentoView confirmado = agendamentoService.confirmarSinalManual(negocio.getId(), criado.id(), AGORA.plusSeconds(120));

        assertThat(confirmado.status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        verify(notificationService).enviar(eq(TELEFONE), contains("está confirmado"));
    }

    @Test
    void servicoSemSinalNaoPrecisaDePixNemDeConfirmacao() {
        CheckoutAgendamento checkout = checkoutService.iniciar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThat(checkout.agendamento().status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(checkout.agendamento().meioCobranca()).isNull();
        assertThat(checkout.cobranca()).isNull();
    }

}
