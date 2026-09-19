package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.castilhodigital.facilitei.common.exception.ConflitoDeHorarioException;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O caminho completo do Pix proprio contra o Postgres real: o cliente paga e avisa, o dono e chamado no WhatsApp
 * com um link secreto, confere o extrato e confirma ou recusa - inclusive quando o prazo acaba no meio disso.
 */
class PixProprioConfirmacaoIntegrationTest extends CenarioPixProprioIntegrationTest {

    private static final Instant INFORMOU_EM = AGORA.plus(Duration.ofMinutes(5));
    private static final Instant DONO_CONFIRMA_EM = INFORMOU_EM.plus(Duration.ofMinutes(20));

    @Autowired
    private AgendamentoCheckoutService checkoutService;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoPagamentoService pagamentoService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private AgendamentoView reservar() {
        return checkoutService.iniciar(pedidoDoCorteAsDez(), AGORA).agendamento();
    }

    private AgendamentoView clienteAvisaQuePagou(AgendamentoView reserva, Instant quando) {
        return pagamentoService.informarPagamentoDoCliente("barbearia", reserva.codigoPublico(), quando);
    }

    /** O token que chegou ao WhatsApp do dono (unico lugar onde ele existe em claro). */
    private String tokenEnviadoAoDono() {
        ArgumentCaptor<String> mensagem = ArgumentCaptor.forClass(String.class);
        verify(notificationService).enviar(eq(TELEFONE_DONO), mensagem.capture());
        return tokenDaMensagem(mensagem.getValue());
    }

    private AgendamentoView reservaJaAvisada() {
        AgendamentoView reserva = reservar();
        clienteAvisaQuePagou(reserva, INFORMOU_EM);
        return reserva;
    }

    private AgendamentoStatus statusDe(Long agendamentoId) {
        return agendamentoRepository.findById(agendamentoId).orElseThrow().getStatus();
    }

    private NovoAgendamento pedidoDeOutraPessoa() {
        return new NovoAgendamento(negocio.getId(), List.of(corte.getId()), ana.getId(), sp(10, 0),
                "Carlos", "63955554444", null, null, AgendamentoOrigem.PUBLICO);
    }

    // ---- o cliente avisa que pagou ----

    @Test
    void clienteAvisaQuePagouEODonoRecebeOLinkParaConferirComOTokenSoNoWhatsApp() {
        AgendamentoView reserva = reservar();
        clearInvocations(notificationService);

        AgendamentoView avisada = clienteAvisaQuePagou(reserva, INFORMOU_EM);

        assertThat(avisada.status()).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        assertThat(avisada.expiraEm()).as("mais folga para o dono conferir").isEqualTo(INFORMOU_EM.plus(Duration.ofMinutes(60)));
        assertThat(avisada.pagamentoInformadoEm()).isEqualTo(INFORMOU_EM);

        ArgumentCaptor<String> mensagem = ArgumentCaptor.forClass(String.class);
        verify(notificationService).enviar(eq(TELEFONE_DONO), mensagem.capture());
        assertThat(mensagem.getValue())
                .contains("João avisou que pagou o sinal de R$ 15,00 (Corte, 21/09 às 10:00)")
                .contains("referência FAC" + reserva.id())
                .contains("http://localhost:4200/confirmar-sinal/");
        verify(notificationService, never()).enviar(eq(TELEFONE), anyString());

        // o banco guarda so o hash: quem tem um dump nao consegue montar o link
        String token = tokenDaMensagem(mensagem.getValue());
        var gravado = agendamentoRepository.findById(reserva.id()).orElseThrow();
        assertThat(gravado.getTokenConfirmacaoHash()).isEqualTo(TokenConfirmacao.hash(token)).isNotEqualTo(token);
    }

    @Test
    void tocarDuasVezesNaoMandaOutroPedidoAoDonoNemTrocaOLink() {
        AgendamentoView reserva = reservar();
        clearInvocations(notificationService);
        clienteAvisaQuePagou(reserva, INFORMOU_EM);
        String hashDoPrimeiro = agendamentoRepository.findById(reserva.id()).orElseThrow().getTokenConfirmacaoHash();

        AgendamentoView segunda = clienteAvisaQuePagou(reserva, INFORMOU_EM.plusSeconds(30));

        assertThat(segunda.status()).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        assertThat(segunda.expiraEm()).as("nao estende o prazo a cada toque").isEqualTo(INFORMOU_EM.plus(Duration.ofMinutes(60)));
        verify(notificationService, times(1)).enviar(eq(TELEFONE_DONO), anyString());
        assertThat(agendamentoRepository.findById(reserva.id()).orElseThrow().getTokenConfirmacaoHash()).isEqualTo(hashDoPrimeiro);
    }

    @Test
    void reservaInexistenteOuDeOutroNegocioNaoPodeSerAvisada() {
        AgendamentoView reserva = reservar();

        assertThatThrownBy(() -> pagamentoService.informarPagamentoDoCliente("barbearia", java.util.UUID.randomUUID(), INFORMOU_EM))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        tenant("outra-barbearia");
        assertThatThrownBy(() -> pagamentoService.informarPagamentoDoCliente("outra-barbearia", reserva.codigoPublico(), INFORMOU_EM))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
    }

    // ---- o dono confirma ou recusa pelo link ----

    @Test
    void donoConfirmaPeloLinkEOClienteERecebeOAvisoUmaSoVez() {
        AgendamentoView reserva = reservaJaAvisada();
        String token = tokenEnviadoAoDono();
        clearInvocations(notificationService);

        AgendamentoView confirmada = agendamentoService.confirmarPorToken(token, DONO_CONFIRMA_EM);
        agendamentoService.confirmarPorToken(token, DONO_CONFIRMA_EM.plusSeconds(5));   // reabriu o link e tocou de novo

        assertThat(confirmada.status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        var gravado = agendamentoRepository.findById(reserva.id()).orElseThrow();
        assertThat(gravado.getSinalConfirmadoEm()).isEqualTo(DONO_CONFIRMA_EM);
        assertThat(gravado.getExpiraEm()).isNull();
        verify(notificationService, times(1)).enviar(eq(TELEFONE), contains("está confirmado"));
    }

    @Test
    void donoRecusaPeloLinkOHorarioVoltaParaAAgendaEOClienteEAvisado() {
        AgendamentoView reserva = reservaJaAvisada();
        String token = tokenEnviadoAoDono();
        clearInvocations(notificationService);

        AgendamentoView recusada = agendamentoService.recusarPorToken(token);
        agendamentoService.recusarPorToken(token);   // tocar de novo nao muda nada nem reavisa

        assertThat(recusada.status()).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
        verify(notificationService, times(1)).enviar(eq(TELEFONE), contains("não encontrou o pagamento do sinal"));
        // o horario esta livre para outra pessoa
        assertThat(agendamentoService.criar(pedidoDeOutraPessoa(), DONO_CONFIRMA_EM).status())
                .isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
    }

    @Test
    void depoisDeDecididoOLinkNaoDesfazAOutraDecisao() {
        AgendamentoView confirmada = reservaJaAvisada();
        String tokenDoConfirmado = tokenEnviadoAoDono();
        agendamentoService.confirmarPorToken(tokenDoConfirmado, DONO_CONFIRMA_EM);
        // "nao recebi" por um link antigo nao pode derrubar uma reserva ja confirmada
        assertThatThrownBy(() -> agendamentoService.recusarPorToken(tokenDoConfirmado))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("já foi confirmado");
        assertThat(statusDe(confirmada.id())).isEqualTo(AgendamentoStatus.CONFIRMADO);
    }

    @Test
    void naoSeConfirmaUmaReservaQueOPropioDonoJaRecusou() {
        AgendamentoView reserva = reservaJaAvisada();
        String token = tokenEnviadoAoDono();
        agendamentoService.recusarPorToken(token);

        assertThatThrownBy(() -> agendamentoService.confirmarPorToken(token, DONO_CONFIRMA_EM))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("cancelado");
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
    }

    // ---- o segredo do link ----

    /** O cliente conhece o codigo da propria reserva, mas so o dono tem o token: se um servisse pelo outro, o cliente se auto-confirmaria. */
    @Test
    void oCodigoPublicoDoClienteNaoConfirmaNada() {
        AgendamentoView reserva = reservaJaAvisada();
        String hashNoBanco = agendamentoRepository.findById(reserva.id()).orElseThrow().getTokenConfirmacaoHash();

        assertThatThrownBy(() -> agendamentoService.confirmarPorToken(reserva.codigoPublico().toString(), DONO_CONFIRMA_EM))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        // e o hash que esta no banco tambem nao vale como token (quem tem um dump nao confirma pagamentos)
        assertThatThrownBy(() -> agendamentoService.confirmarPorToken(hashNoBanco, DONO_CONFIRMA_EM))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        assertThatThrownBy(() -> agendamentoService.recusarPorToken("x".repeat(43)))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
    }

    @Test
    void tokenDeUmaReservaNaoMexeEmOutra() {
        AgendamentoView primeira = reservaJaAvisada();
        String tokenDaPrimeira = tokenEnviadoAoDono();
        // segunda reserva de outra pessoa, em outro horario
        AgendamentoView segunda = agendamentoService.criar(new NovoAgendamento(negocio.getId(), List.of(corte.getId()),
                ana.getId(), sp(11, 0), "Carlos", "63955554444", null, null, AgendamentoOrigem.PUBLICO), AGORA);

        agendamentoService.confirmarPorToken(tokenDaPrimeira, DONO_CONFIRMA_EM);

        assertThat(statusDe(primeira.id())).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(statusDe(segunda.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
    }

    // ---- o prazo acaba no meio do caminho ----

    @Test
    void prazoDeConfirmacaoDuraUmaHoraEDepoisAvisaOClienteSemDizerQueEleNaoPagou() {
        AgendamentoView reserva = reservaJaAvisada();
        clearInvocations(notificationService);

        assertThat(agendamentoService.expirarVencidos(INFORMOU_EM.plus(Duration.ofMinutes(59)))).isZero();
        assertThat(agendamentoService.expirarVencidos(INFORMOU_EM.plus(Duration.ofMinutes(61)))).isEqualTo(1);

        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.EXPIRADO);
        verify(notificationService).enviar(eq(TELEFONE), contains("não confirmou o seu pagamento"));
    }

    @Test
    void donoQueDemorouAindaConfirmaSeOHorarioContinuaLivre() {
        AgendamentoView reserva = reservaJaAvisada();
        String token = tokenEnviadoAoDono();
        agendamentoService.expirarVencidos(INFORMOU_EM.plus(Duration.ofMinutes(61)));

        AgendamentoView confirmada = agendamentoService.confirmarPorToken(token, INFORMOU_EM.plus(Duration.ofMinutes(90)));

        assertThat(confirmada.status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.CONFIRMADO);
    }

    /** O Pix caiu na conta do dono, mas outra pessoa ja pegou o horario: a reserva dela e sagrada, o dono e mandado devolver. */
    @Test
    void donoQueDemorouNaoConfirmaSeOutraPessoaJaTomouOHorario() {
        AgendamentoView reserva = reservaJaAvisada();
        String token = tokenEnviadoAoDono();
        Instant depoisDoPrazo = INFORMOU_EM.plus(Duration.ofMinutes(61));
        agendamentoService.expirarVencidos(depoisDoPrazo);
        AgendamentoView outraPessoa = agendamentoService.criar(pedidoDeOutraPessoa(), depoisDoPrazo);

        assertThatThrownBy(() -> agendamentoService.confirmarPorToken(token, depoisDoPrazo.plusSeconds(60)))
                .isInstanceOf(ConflitoDeHorarioException.class)
                .hasMessageContaining("Devolva o sinal ao cliente");

        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.EXPIRADO);
        assertThat(statusDe(outraPessoa.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
    }

    // ---- o cliente avisa depois de o prazo ja ter passado ----

    @Test
    void clienteQueAvisaDepoisDeExpirarRetomaAEsperaSeOHorarioContinuaLivre() {
        AgendamentoView reserva = reservar();
        Instant depoisDoPrazo = AGORA.plus(Duration.ofMinutes(20));
        agendamentoService.expirarVencidos(depoisDoPrazo);
        clearInvocations(notificationService);

        AgendamentoView avisada = clienteAvisaQuePagou(reserva, depoisDoPrazo);

        assertThat(avisada.status()).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
        verify(notificationService).enviar(eq(TELEFONE_DONO), contains("Pix a conferir"));
    }

    @Test
    void clienteQueAvisaDepoisDeExpirarComOHorarioOcupadoLevaConflitoEODonoEAlertadoParaDevolver() {
        AgendamentoView reserva = reservar();
        Instant depoisDoPrazo = AGORA.plus(Duration.ofMinutes(20));
        agendamentoService.expirarVencidos(depoisDoPrazo);
        AgendamentoView outraPessoa = agendamentoService.criar(pedidoDeOutraPessoa(), depoisDoPrazo);
        clearInvocations(notificationService);

        assertThatThrownBy(() -> clienteAvisaQuePagou(reserva, depoisDoPrazo.plusSeconds(30)))
                .isInstanceOf(ConflitoDeHorarioException.class)
                .hasMessageContaining("Se você já pagou, fale com o estabelecimento");

        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.EXPIRADO);
        assertThat(statusDe(outraPessoa.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        assertThat(agendamentoRepository.findById(reserva.id()).orElseThrow().getTokenConfirmacaoHash())
                .as("sem reserva para confirmar, nao ha link").isNull();
        ArgumentCaptor<String> alerta = ArgumentCaptor.forClass(String.class);
        verify(notificationService).enviar(eq(TELEFONE_DONO), alerta.capture());
        assertThat(alerta.getValue())
                .startsWith("Atenção: João, fone (63) 99999-9999, avisou que pagou o sinal de R$ 15,00 do horário de 21/09 às 10:00")
                .contains("o horário foi ocupado por outra pessoa")
                .contains("devolva o valor ao cliente")
                .doesNotContain("confirmar-sinal");
    }

    // ---- token substituido ----

    @Test
    void novoAvisoDepoisDeExpirarGeraOutroLinkEOAntigoDeixaDeValer() {
        AgendamentoView reserva = reservaJaAvisada();
        String tokenAntigo = tokenEnviadoAoDono();
        Instant depoisDoPrazo = INFORMOU_EM.plus(Duration.ofMinutes(61));
        agendamentoService.expirarVencidos(depoisDoPrazo);
        clearInvocations(notificationService);

        clienteAvisaQuePagou(reserva, depoisDoPrazo.plusSeconds(60));
        String tokenNovo = tokenEnviadoAoDono();

        assertThat(tokenNovo).isNotEqualTo(tokenAntigo);
        assertThatThrownBy(() -> agendamentoService.buscarPorTokenDoDono(tokenAntigo))
                .isInstanceOf(EntidadeNaoEncontradaException.class).hasMessageContaining("substituído");
        assertThat(agendamentoService.buscarPorTokenDoDono(tokenNovo).id()).isEqualTo(reserva.id());
    }

    // ---- painel ----

    @Test
    void painelConfirmaOPagamentoDeQuemAvisou() {
        AgendamentoView reserva = reservaJaAvisada();

        AgendamentoView confirmada = agendamentoService.confirmarSinalManual(negocio.getId(), reserva.id(), DONO_CONFIRMA_EM);

        assertThat(confirmada.status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
    }

    @Test
    void painelRecusaOPagamentoDeQuemAvisouEAvisaOCliente() {
        AgendamentoView reserva = reservaJaAvisada();
        clearInvocations(notificationService);

        AgendamentoView recusada = agendamentoService.recusarPagamentoManual(negocio.getId(), reserva.id());

        assertThat(recusada.status()).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
        verify(notificationService).enviar(eq(TELEFONE), contains("não encontrou o pagamento"));
    }

    @Test
    void painelNaoRecusaQuemAindaNaoAvisouQuePagouEOrientaCancelar() {
        AgendamentoView reserva = reservar();

        assertThatThrownBy(() -> agendamentoService.recusarPagamentoManual(negocio.getId(), reserva.id()))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("cancele");
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void painelDeOutroNegocioNaoAlcancaAReserva() {
        AgendamentoView reserva = reservaJaAvisada();
        Long outroNegocio = tenant("concorrente").getId();

        assertThatThrownBy(() -> agendamentoService.confirmarSinalManual(outroNegocio, reserva.id(), DONO_CONFIRMA_EM))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        assertThatThrownBy(() -> agendamentoService.recusarPagamentoManual(outroNegocio, reserva.id()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    // ---- lock: o job de expiracao nao atropela quem esta decidindo ----

    /**
     * O dono confirmando (ou recusando) e o job de expiracao mexem na mesma linha. Se as buscas de quem decide nao
     * travassem a linha, os dois leriam "esperando" e o ultimo a gravar venceria - "confirmado" e "expirado" ao mesmo
     * tempo, com o horario ja entregue a outra pessoa. Com o lock, o job (SKIP LOCKED) pula a linha em uso.
     */
    @Test
    void jobDeExpiracaoPulaAReservaQueOPainelEstaDecidindo() {
        AgendamentoView reserva = reservar();
        Instant depoisDoPrazo = AGORA.plus(Duration.ofMinutes(16));

        int expiradosEnquantoOPainelSegurava = new TransactionTemplate(transactionManager).execute(tx -> {
            agendamentoRepository.findWithLockByIdAndTenantId(reserva.id(), negocio.getId()).orElseThrow();
            return CompletableFuture.supplyAsync(() -> agendamentoService.expirarVencidos(depoisDoPrazo)).join();
        });

        assertThat(expiradosEnquantoOPainelSegurava).isZero();
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        assertThat(agendamentoService.expirarVencidos(depoisDoPrazo)).as("solto o lock, o job faz o seu trabalho").isEqualTo(1);
    }

    @Test
    void jobDeExpiracaoPulaAReservaQueODonoEstaDecidindoPeloLink() {
        AgendamentoView reserva = reservaJaAvisada();
        String token = tokenEnviadoAoDono();
        Instant depoisDoPrazo = INFORMOU_EM.plus(Duration.ofMinutes(61));

        int expiradosEnquantoODonoSegurava = new TransactionTemplate(transactionManager).execute(tx -> {
            agendamentoRepository.findWithLockByTokenConfirmacaoHash(TokenConfirmacao.hash(token)).orElseThrow();
            return CompletableFuture.supplyAsync(() -> agendamentoService.expirarVencidos(depoisDoPrazo)).join();
        });

        assertThat(expiradosEnquantoODonoSegurava).isZero();
        assertThat(statusDe(reserva.id())).isEqualTo(AgendamentoStatus.AGUARDANDO_CONFIRMACAO);
    }

}
