package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.exception.ConflitoDeHorarioException;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ciclo de vida do agendamento contra o Postgres real: criacao, sinal, checkout,
 * expiracao, acoes do painel e notificacoes depois do commit.
 *
 * Cenario (segunda 2026-09-21; "agora" = domingo anterior 12:00): Ana faz Corte, Barba e Lavagem
 * (09-12 e 14-18); Bia faz Corte e Lavagem (10-16). Corte e Barba cobram 30% de sinal; Lavagem nao cobra.
 */
class AgendamentoServiceIntegrationTest extends AgendaIntegrationTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 9, 21);
    private static final Instant AGORA = LocalDate.of(2026, 9, 20).atTime(12, 0).atZone(SAO_PAULO).toInstant();
    private static final String TELEFONE_DIGITADO = "(63) 99999-9999";
    private static final String TELEFONE = "5563999999999";
    private static final String CPF_VALIDO = "52998224725";

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoCheckoutService checkoutService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Tenant negocio;
    private ServiceOffering corte;
    private ServiceOffering barba;
    private ServiceOffering lavagem;
    private Profissional ana;
    private Profissional bia;

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

    private static Instant sp(int hora, int minuto) {
        return SEGUNDA.atTime(hora, minuto).atZone(SAO_PAULO).toInstant();
    }

    private NovoAgendamento pedido(List<Long> servicoIds, Long profissionalId, Instant inicio) {
        return new NovoAgendamento(negocio.getId(), servicoIds, profissionalId, inicio,
                "João", TELEFONE_DIGITADO, CPF_VALIDO, null, AgendamentoOrigem.PUBLICO);
    }

    private NovoAgendamento pedidoDoPainel(List<Long> servicoIds, Long profissionalId, Instant inicio) {
        return new NovoAgendamento(negocio.getId(), servicoIds, profissionalId, inicio,
                "Maria", "63988887777", null, null, AgendamentoOrigem.PAINEL);
    }

    // ---- criacao ----

    @Test
    void semSinalNasceConfirmadoEAvisaOClienteDepoisDoCommit() {
        AgendamentoView criado = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThat(criado.status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(criado.valorTotal()).isEqualByComparingTo("20.00");
        assertThat(criado.valorSinal()).isEqualByComparingTo("0.00");
        assertThat(criado.expiraEm()).isNull();
        assertThat(criado.inicio()).isEqualTo(sp(10, 0));
        assertThat(criado.fim()).isEqualTo(sp(10, 30));
        assertThat(criado.clienteTelefone()).as("guarda o telefone normalizado").isEqualTo(TELEFONE);
        assertThat(criado.codigoPublico()).isNotNull();
        assertThat(criado.servicos()).extracting(AgendamentoView.Servico::nome).containsExactly("Lavagem");

        verify(notificationService).enviar(eq(TELEFONE), contains("está confirmado: 21/09 às 10:00"));
    }

    @Test
    void comSinalSeguraOHorarioPorQuinzeMinutosECalculaOSinalServicoAServico() {
        AgendamentoView criado = agendamentoService.criar(
                pedido(List.of(corte.getId(), barba.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThat(criado.status()).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        assertThat(criado.valorTotal()).isEqualByComparingTo("85.00");
        // 30% de 50,00 = 15,00 e 30% de 35,00 = 10,50
        assertThat(criado.valorSinal()).isEqualByComparingTo("25.50");
        assertThat(criado.expiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(15)));
        assertThat(criado.fim()).as("duas vezes 30min").isEqualTo(sp(11, 0));
        assertThat(criado.servicos()).extracting(AgendamentoView.Servico::nome).containsExactly("Corte", "Barba");

        // o cliente so e avisado depois de a cobranca existir (ver checkout), nunca de uma reserva que pode ser desfeita
        verifyNoInteractions(notificationService);
    }

    @Test
    void qualquerProfissionalRecebeOPrimeiroLivreENaoOAgendadoPorOutro() {
        agendamentoService.criar(pedido(List.of(lavagem.getId()), null, sp(10, 0)), AGORA);
        AgendamentoView segundo = agendamentoService.criar(pedido(List.of(lavagem.getId()), null, sp(10, 0)), AGORA);

        assertThat(segundo.profissionalNome()).as("Ana ja esta ocupada as 10:00").isEqualTo("Bia");
    }

    @Test
    void inicioQueAAgendaNaoOfereceEConflitoENadaEGravado() {
        assertThatThrownBy(() -> agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(9, 10)), AGORA))
                .as("fora da grade de 30 minutos").isInstanceOf(ConflitoDeHorarioException.class);
        assertThatThrownBy(() -> agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(13, 0)), AGORA))
                .as("no almoco da Ana").isInstanceOf(ConflitoDeHorarioException.class);
        assertThatThrownBy(() -> agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(3, 0)), AGORA))
                .as("madrugada").isInstanceOf(ConflitoDeHorarioException.class);

        assertThat(agendamentoRepository.count()).isZero();
        verifyNoInteractions(notificationService);
    }

    @Test
    void segundoClienteNoMesmoHorarioRecebeConflitoESemNotificacaoNenhuma() {
        agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThatThrownBy(() -> agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA))
                .isInstanceOf(ConflitoDeHorarioException.class);

        verify(notificationService, times(1)).enviar(anyString(), anyString());
        assertThat(agendamentoRepository.count()).isEqualTo(1);
    }

    /** O cenario que a agenda antiga nao resistia: N requisicoes ao mesmo tempo pelo mesmo horario. */
    @Test
    void corridaSimultaneaPeloMesmoHorarioTemUmSoVencedor() throws Exception {
        int tentativas = 8;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<String>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            resultados.add(pool.submit(() -> {
                largada.await();
                try {
                    agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);
                    return "ok";
                } catch (ConflitoDeHorarioException ex) {
                    return "conflito";
                }
            }));
        }
        largada.countDown();

        List<String> saidas = new ArrayList<>();
        for (Future<String> resultado : resultados) {
            saidas.add(resultado.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(saidas.stream().filter("ok"::equals).count()).as("vencedores").isEqualTo(1);
        assertThat(saidas.stream().filter("conflito"::equals).count()).as("perdedores").isEqualTo(tentativas - 1);
        assertThat(agendamentoRepository.count()).isEqualTo(1);
    }

    /**
     * O defeito da agenda antiga: o WhatsApp saia no meio da transacao, entao um rollback deixava o cliente com o
     * aviso de uma reserva que nao existia. Aqui o evento e publicado e SO ENTAO a transacao inteira e desfeita.
     */
    @Test
    void rollbackDepoisDePublicarOEventoNaoNotificaNinguem() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            agendamentoService.criarPeloPainel(pedidoDoPainel(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);
            throw new IllegalStateException("falha depois de publicar o evento");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(agendamentoRepository.count()).as("o agendamento foi desfeito").isZero();
        verifyNoInteractions(notificationService);
    }

    @Test
    void telefoneInvalidoEhRecusado() {
        NovoAgendamento comTelefoneRuim = new NovoAgendamento(negocio.getId(), List.of(lavagem.getId()), ana.getId(),
                sp(10, 0), "João", "12345", CPF_VALIDO, null, AgendamentoOrigem.PUBLICO);

        assertThatThrownBy(() -> agendamentoService.criar(comTelefoneRuim, AGORA))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("telefone");
    }

    // ---- limites e validacoes antes de segurar o horario ----

    @Test
    void mesmoTelefoneNaoPodeSegurarMaisDeDuasReservasSemPagar() {
        agendamentoService.criar(pedido(List.of(corte.getId()), ana.getId(), sp(9, 0)), AGORA);
        agendamentoService.criar(pedido(List.of(corte.getId()), ana.getId(), sp(9, 30)), AGORA);

        assertThatThrownBy(() -> agendamentoService.criar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA))
                .isInstanceOf(LimiteDeRequisicoesExcedidoException.class);

        // outro telefone nao e afetado
        NovoAgendamento outraPessoa = new NovoAgendamento(negocio.getId(), List.of(corte.getId()), ana.getId(),
                sp(10, 0), "Pedro", "(63) 98888-1111", CPF_VALIDO, null, AgendamentoOrigem.PUBLICO);
        assertThatCode(() -> agendamentoService.criar(outraPessoa, AGORA)).doesNotThrowAnyException();
    }

    @Test
    void reservasSemSinalNaoContamNoLimiteDeReservasEmEspera() {
        agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(9, 0)), AGORA);  // sem sinal: confirmada
        agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(9, 30)), AGORA);
        agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThat(agendamentoRepository.count()).isEqualTo(3);
    }

    @Test
    void negocioSemFormaDeReceberNaoSeguraOHorarioDeUmServicoComSinal() {
        negocio.setAsaasApiKey(null);
        tenantRepository.saveAndFlush(negocio);

        assertThatThrownBy(() -> agendamentoService.criar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("recebimento do sinal");

        assertThat(agendamentoRepository.count()).isZero();
    }

    @Test
    void semCpfValidoOSinalPelaAsaasEhRecusadoAntesDeSegurarOHorario() {
        NovoAgendamento semCpf = new NovoAgendamento(negocio.getId(), List.of(corte.getId()), ana.getId(),
                sp(10, 0), "João", TELEFONE_DIGITADO, null, null, AgendamentoOrigem.PUBLICO);

        assertThatThrownBy(() -> agendamentoService.criar(semCpf, AGORA))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("CPF");

        assertThat(agendamentoRepository.count()).isZero();
    }

    // ---- painel ----

    @Test
    void painelIgnoraExpedienteMasNuncaPermiteSobreposicao() {
        // 20:00 esta fora do expediente da Ana - o dono pode encaixar mesmo assim
        AgendamentoView encaixe = agendamentoService.criarPeloPainel(pedidoDoPainel(List.of(corte.getId()), ana.getId(), sp(20, 0)), AGORA);

        assertThat(encaixe.status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(encaixe.origem()).isEqualTo(AgendamentoOrigem.PAINEL);
        assertThat(encaixe.valorSinal()).as("o painel nao cobra sinal, mesmo o servico tendo percentual").isEqualByComparingTo("0.00");

        // e a constraint do banco continua valendo: mesmo horario de novo e conflito
        assertThatThrownBy(() -> agendamentoService.criarPeloPainel(pedidoDoPainel(List.of(corte.getId()), ana.getId(), sp(20, 15)), AGORA))
                .isInstanceOf(ConflitoDeHorarioException.class);
    }

    @Test
    void painelExigeProfissionalEQueEleRealizeOsServicos() {
        assertThatThrownBy(() -> agendamentoService.criarPeloPainel(pedidoDoPainel(List.of(corte.getId()), null, sp(10, 0)), AGORA))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> agendamentoService.criarPeloPainel(pedidoDoPainel(List.of(barba.getId()), bia.getId(), sp(10, 0)), AGORA))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("nao realiza");
    }

    // ---- checkout (sinal) ----

    @Test
    void checkoutCriaACobrancaNaContaDoNegocioEAvisaOClienteSoDepoisDeVincula() {
        CheckoutAgendamento checkout = checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThat(checkout.agendamento().status()).isEqualTo(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
        assertThat(checkout.agendamento().pixPayload()).isEqualTo("00020126payload");
        assertThat(checkout.agendamento().cobrancaExternaId()).isEqualTo("pay_1");
        assertThat(checkout.cobranca().qrCodeBase64()).isEqualTo("base64img");

        ArgumentCaptor<PixChargeRequest> cobranca = ArgumentCaptor.forClass(PixChargeRequest.class);
        verify(paymentGatewayService).criarCobrancaPix(eq("chave-asaas"), cobranca.capture());
        assertThat(cobranca.getValue().valor()).isEqualByComparingTo("15.00");
        assertThat(cobranca.getValue().referenciaExterna()).isEqualTo("agendamento-" + checkout.agendamento().id());
        assertThat(cobranca.getValue().clienteCpfCnpj()).isEqualTo(CPF_VALIDO);

        verify(notificationService).enviar(eq(TELEFONE), contains("Pague o sinal via Pix até às 12:15"));
    }

    @Test
    void checkoutSemSinalNemChamaOGateway() {
        CheckoutAgendamento checkout = checkoutService.iniciar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThat(checkout.agendamento().status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(checkout.cobranca()).isNull();
        verifyNoInteractions(paymentGatewayService);
    }

    @Test
    void seOGatewayFalharOHorarioVoltaNaHoraESemAvisarOCliente() {
        when(paymentGatewayService.criarCobrancaPix(anyString(), any(PixChargeRequest.class)))
                .thenThrow(new PaymentGatewayException("Asaas fora do ar", null));

        assertThatThrownBy(() -> checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA))
                .isInstanceOf(PaymentGatewayException.class);

        assertThat(agendamentoRepository.findAll()).singleElement()
                .satisfies(agendamento -> assertThat(agendamento.getStatus()).isEqualTo(AgendamentoStatus.EXPIRADO));
        verifyNoInteractions(notificationService);
        // o horario esta livre de novo: outro cliente consegue reserva-lo
        assertThatCode(() -> agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA))
                .doesNotThrowAnyException();
    }

    // ---- expiracao ----

    @Test
    void expirarVencidosLiberaSoQuemPassouDoPrazoEAvisaDepoisDoCommit() {
        agendamentoService.criar(pedido(List.of(corte.getId()), ana.getId(), sp(9, 0)), AGORA);          // esperando, vence 12:15 de domingo
        agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(9, 30)), AGORA);       // confirmado
        Instant depoisDoPrazo = AGORA.plus(Duration.ofMinutes(16));

        assertThat(agendamentoService.expirarVencidos(AGORA.plus(Duration.ofMinutes(10)))).as("ainda dentro do prazo").isZero();
        assertThat(agendamentoService.expirarVencidos(depoisDoPrazo)).isEqualTo(1);
        assertThat(agendamentoService.expirarVencidos(depoisDoPrazo)).as("segunda execucao nao repete").isZero();

        assertThat(agendamentoRepository.findAll()).extracting(Agendamento::getStatus)
                .containsExactlyInAnyOrder(AgendamentoStatus.EXPIRADO, AgendamentoStatus.CONFIRMADO);
        verify(notificationService).enviar(eq(TELEFONE), contains("prazo para pagar o sinal"));
        // o horario voltou
        assertThatCode(() -> agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(9, 0)), depoisDoPrazo))
                .doesNotThrowAnyException();
    }

    // ---- acoes do painel ----

    @Test
    void donoConfirmaOSinalManualmenteEReenviarNaoDuplicaAviso() {
        AgendamentoView esperando = agendamentoService.criar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA);

        AgendamentoView confirmado = agendamentoService.confirmarSinalManual(negocio.getId(), esperando.id(), AGORA);
        agendamentoService.confirmarSinalManual(negocio.getId(), esperando.id(), AGORA);

        assertThat(confirmado.status()).isEqualTo(AgendamentoStatus.CONFIRMADO);
        assertThat(confirmado.expiraEm()).isNull();
        verify(notificationService, times(1)).enviar(eq(TELEFONE), contains("está confirmado"));
    }

    @Test
    void cancelarPeloNegocioLiberaOHorarioEAvisa() {
        AgendamentoView confirmado = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        AgendamentoView cancelado = agendamentoService.cancelarPeloNegocio(negocio.getId(), confirmado.id());

        assertThat(cancelado.status()).isEqualTo(AgendamentoStatus.CANCELADO_NEGOCIO);
        verify(notificationService).enviar(eq(TELEFONE), contains("cancelado pelo estabelecimento"));
        assertThatCode(() -> agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA))
                .doesNotThrowAnyException();
    }

    @Test
    void concluirEMarcarFaltaSoDepoisDeOAtendimentoComecar() {
        AgendamentoView primeiro = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);
        AgendamentoView segundo = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(11, 0)), AGORA);

        assertThatThrownBy(() -> agendamentoService.concluir(negocio.getId(), primeiro.id(), sp(9, 0)))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThat(agendamentoService.concluir(negocio.getId(), primeiro.id(), sp(10, 30)).status())
                .isEqualTo(AgendamentoStatus.CONCLUIDO);
        assertThat(agendamentoService.marcarFalta(negocio.getId(), segundo.id(), sp(11, 30)).status())
                .isEqualTo(AgendamentoStatus.FALTOU);
    }

    @Test
    void negocioNaoMexeNoAgendamentoDeOutroNegocio() {
        Tenant concorrente = tenant("concorrente");
        AgendamentoView meu = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        assertThatThrownBy(() -> agendamentoService.cancelarPeloNegocio(concorrente.getId(), meu.id()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        assertThatThrownBy(() -> agendamentoService.buscarDoTenant(concorrente.getId(), meu.id()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    // ---- consultas ----

    @Test
    void consultaPublicaPorCodigoSoValeDentroDoNegocioDoLink() {
        AgendamentoView criado = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);
        tenant("concorrente");

        assertThat(agendamentoService.buscarPorCodigoPublico("barbearia", criado.codigoPublico()).id()).isEqualTo(criado.id());
        assertThatThrownBy(() -> agendamentoService.buscarPorCodigoPublico("concorrente", criado.codigoPublico()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
        assertThatThrownBy(() -> agendamentoService.buscarPorCodigoPublico("barbearia", UUID.randomUUID()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void agendaDoPainelListaTudoDoPeriodoEmOrdemEFiltraPorProfissional() {
        agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(14, 0)), AGORA);
        agendamentoService.criar(pedido(List.of(corte.getId(), barba.getId()), ana.getId(), sp(10, 0)), AGORA);
        AgendamentoView daBia = agendamentoService.criar(pedido(List.of(lavagem.getId()), bia.getId(), sp(11, 0)), AGORA);
        agendamentoService.cancelarPeloNegocio(negocio.getId(), daBia.id());

        List<AgendamentoView> todos = agendamentoService.listarAgenda(negocio.getId(), null, SEGUNDA, SEGUNDA);
        List<AgendamentoView> soDaAna = agendamentoService.listarAgenda(negocio.getId(), ana.getId(), SEGUNDA, SEGUNDA);

        assertThat(todos).extracting(AgendamentoView::inicio).containsExactly(sp(10, 0), sp(11, 0), sp(14, 0));
        assertThat(todos).extracting(AgendamentoView::status).contains(AgendamentoStatus.CANCELADO_NEGOCIO);
        assertThat(todos.get(0).servicos()).extracting(AgendamentoView.Servico::nome).containsExactly("Corte", "Barba");
        assertThat(soDaAna).extracting(AgendamentoView::profissionalNome).containsOnly("Ana").hasSize(2);
        assertThat(agendamentoService.listarAgenda(negocio.getId(), null, SEGUNDA.plusDays(1), SEGUNDA.plusDays(1))).isEmpty();
    }


}
