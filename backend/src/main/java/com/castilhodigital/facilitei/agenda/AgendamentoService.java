package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.DisponibilidadeService.HorarioDisponivel;
import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinal;
import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinalResolver;
import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinalService;
import com.castilhodigital.facilitei.agenda.sinal.SolicitacaoCobranca;
import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.PostgresErros;
import com.castilhodigital.facilitei.common.exception.ConflitoDeHorarioException;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.LimiteDeRequisicoesExcedidoException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.common.validation.TelefoneBrasil;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.professional.ProfissionalService;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.tenant.TenantService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria e conduz o ciclo de vida dos agendamentos. Nao ha "reservar slot" aqui:
 * a corrida entre dois clientes pelo mesmo horario e decidida pela constraint
 * de exclusao do Postgres no INSERT, e a colisao vira ConflitoDeHorarioException
 * (409). A regra de cada transicao de estado fica na entidade Agendamento.
 *
 * Nada aqui chama a rede: cobrar o sinal num gateway e trabalho de
 * AgendamentoCheckoutService, fora da transacao, para nao segurar uma conexao
 * do banco durante uma chamada HTTP nem deixar cobranca orfa num rollback.
 */
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    /** Quanto o horario fica segurado esperando o sinal quando a confirmacao e automatica (webhook do gateway). */
    static final Duration PRAZO_PAGAMENTO_AUTOMATICO = Duration.ofMinutes(15);

    /**
     * Quanto o horario fica segurado depois de o cliente avisar que pagou (Pix proprio), esperando o dono conferir o
     * extrato e confirmar. Mais folgado que o prazo do pagamento: o dono pode estar atendendo e so olhar o celular depois.
     */
    static final Duration PRAZO_CONFIRMACAO_MANUAL = Duration.ofMinutes(60);

    /** Limite de reservas segurando horario sem pagar, por telefone e negocio - contra quem "esgota" a agenda. */
    static final int MAX_RESERVAS_EM_ESPERA_POR_TELEFONE = 2;

    private static final String MENSAGEM_HORARIO_OCUPADO_NA_CONFIRMACAO =
            "O horário deste agendamento já foi ocupado por outra pessoa e não pode mais ser confirmado. Devolva o sinal ao cliente.";
    private static final String MENSAGEM_HORARIO_OCUPADO_NO_AVISO =
            "O prazo para pagar acabou e outra pessoa reservou este horário. Se você já pagou, fale com o estabelecimento: o sinal será devolvido.";

    private static final int LOTE_DE_EXPIRACAO = 200;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final TenantService tenantService;
    private final ProfissionalService profissionalService;
    private final DisponibilidadeService disponibilidadeService;
    private final AgendamentoRepository agendamentoRepository;
    private final CobrancaSinalResolver cobrancaSinalResolver;
    private final ApplicationEventPublisher eventPublisher;

    // ---- criacao ----

    /**
     * Agendamento do cliente (link publico ou portal): so aceita um inicio que a
     * agenda realmente oferece (expediente, bloqueios, grade, antecedencia, horizonte).
     * Sem sinal nasce CONFIRMADO; com sinal segura o horario por um prazo esperando
     * o pagamento (AGUARDANDO_PAGAMENTO) - a cobranca em si sai depois, em AgendamentoCheckoutService.
     */
    @Transactional
    public AgendamentoView criar(NovoAgendamento pedido, Instant agora) {
        if (pedido.origem() == AgendamentoOrigem.PAINEL) {
            throw new IllegalArgumentException("Agendamento do painel usa criarPeloPainel.");
        }
        Tenant tenant = tenantService.buscarPorId(pedido.tenantId());
        String telefone = TelefoneBrasil.normalizar(pedido.clienteTelefone());
        ZoneId fuso = ZoneId.of(tenant.getFusoHorario());

        HorarioDisponivel horario = disponibilidadeService
                .listar(pedido.tenantId(), pedido.servicoIds(), pedido.profissionalId(),
                        pedido.inicio().atZone(fuso).toLocalDate(), agora)
                .stream()
                .filter(candidato -> candidato.inicio().equals(pedido.inicio()))
                .findFirst()
                .orElseThrow(() -> new ConflitoDeHorarioException("Este horário não está mais disponível. Escolha outro."));

        limitarReservasEmEspera(tenant.getId(), telefone);

        List<ServiceOffering> servicos = disponibilidadeService.buscarServicosAtivos(pedido.tenantId(), pedido.servicoIds());
        Profissional profissional = profissionalService.buscarPorIdETenant(pedido.tenantId(), horario.profissionalId());
        Agendamento agendamento = montar(tenant, profissional, servicos, pedido, telefone, horario.inicio(), horario.fim());

        if (agendamento.getValorSinal().signum() > 0) {
            // falhar aqui (negocio sem forma de receber, CPF invalido) e barato: ainda nao seguramos o horario
            CobrancaSinalService meioDeCobranca = cobrancaSinalResolver.exigir(tenant);
            meioDeCobranca.validar(new SolicitacaoCobranca(
                    null, pedido.clienteNome(), telefone, pedido.clienteCpfCnpj(), agendamento.getValorSinal()));
            agendamento.setMeioCobranca(meioDeCobranca.meio());
            agendamento.setStatus(AgendamentoStatus.AGUARDANDO_PAGAMENTO);
            agendamento.setExpiraEm(agora.plus(PRAZO_PAGAMENTO_AUTOMATICO));
        } else {
            agendamento.setStatus(AgendamentoStatus.CONFIRMADO);
        }

        AgendamentoView view = AgendamentoView.de(gravar(agendamento));
        if (view.status() == AgendamentoStatus.CONFIRMADO) {
            publicar(AgendamentoEvento.Tipo.CONFIRMADO, view);
        }
        return view;
    }

    /**
     * Agendamento criado pelo dono/funcionario no painel (cliente que ligou ou
     * chegou no balcao). Vale a palavra do dono: ignora expediente, bloqueio,
     * grade e antecedencia (pode encaixar um cliente fora do horario), nao cobra
     * sinal e nasce CONFIRMADO. Continua protegido contra sobreposicao pelo banco.
     */
    @Transactional
    public AgendamentoView criarPeloPainel(NovoAgendamento pedido, Instant agora) {
        if (pedido.profissionalId() == null) {
            throw new RegraDeNegocioException("Escolha o profissional.");
        }
        Tenant tenant = tenantService.buscarPorId(pedido.tenantId());
        String telefone = TelefoneBrasil.normalizar(pedido.clienteTelefone());
        List<ServiceOffering> servicos = disponibilidadeService.buscarServicosAtivos(pedido.tenantId(), pedido.servicoIds());
        Profissional profissional = disponibilidadeService
                .buscarProfissionalHabilitado(pedido.tenantId(), pedido.profissionalId(), servicos);

        Instant fim = pedido.inicio().plus(Duration.ofMinutes(servicos.stream().mapToInt(ServiceOffering::getDuracaoMin).sum()));
        Agendamento agendamento = montar(tenant, profissional, servicos, pedido, telefone, pedido.inicio(), fim);
        agendamento.setOrigem(AgendamentoOrigem.PAINEL);
        agendamento.setValorSinal(BigDecimal.ZERO);
        agendamento.setStatus(AgendamentoStatus.CONFIRMADO);

        AgendamentoView view = AgendamentoView.de(gravar(agendamento));
        publicar(AgendamentoEvento.Tipo.CONFIRMADO, view);
        return view;
    }

    private Agendamento montar(Tenant tenant, Profissional profissional, List<ServiceOffering> servicos,
                               NovoAgendamento pedido, String telefone, Instant inicio, Instant fim) {
        Agendamento agendamento = new Agendamento();
        agendamento.setTenant(tenant);
        agendamento.setProfissional(profissional);
        agendamento.setCliente(pedido.cliente());
        agendamento.setClienteNome(pedido.clienteNome());
        agendamento.setClienteTelefone(telefone);
        agendamento.setClienteCpfCnpj(pedido.clienteCpfCnpj());
        agendamento.setInicio(inicio);
        agendamento.setFim(fim);
        agendamento.setOrigem(pedido.origem());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal sinal = BigDecimal.ZERO;
        int ordem = 1;
        for (ServiceOffering servico : servicos) {
            agendamento.adicionarServico(AgendamentoServico.copiaDe(servico, ordem++));
            total = total.add(servico.getPreco());
            sinal = sinal.add(servico.getPreco().multiply(servico.getSinalPercentual()).divide(CEM, 2, RoundingMode.HALF_UP));
        }
        agendamento.setValorTotal(total);
        agendamento.setValorSinal(sinal);
        return agendamento;
    }

    /** A constraint de exclusao do banco e quem decide a corrida; aqui so traduzimos a colisao em 409. */
    private Agendamento gravar(Agendamento agendamento) {
        return gravar(agendamento, "Este horário acabou de ser ocupado. Escolha outro.");
    }

    private Agendamento gravar(Agendamento agendamento, String mensagemDeConflito) {
        try {
            return agendamentoRepository.saveAndFlush(agendamento);
        } catch (DataIntegrityViolationException ex) {
            if (PostgresErros.ehViolacaoDeExclusao(ex)) {
                throw new ConflitoDeHorarioException(mensagemDeConflito);
            }
            throw ex;
        }
    }

    private void limitarReservasEmEspera(Long tenantId, String telefone) {
        long emEspera = agendamentoRepository.countByTenantIdAndClienteTelefoneAndStatusIn(
                tenantId, telefone, AgendamentoStatus.EM_ESPERA);
        if (emEspera >= MAX_RESERVAS_EM_ESPERA_POR_TELEFONE) {
            throw new LimiteDeRequisicoesExcedidoException(
                    "Você já tem reservas aguardando pagamento neste negócio. Conclua o pagamento ou espere o prazo acabar para reservar outro horário.");
        }
    }

    // ---- cobranca do sinal (o checkout faz a chamada de rede entre estas duas etapas) ----

    /** Guarda a cobranca criada no gateway e, so agora que ela existe, avisa o cliente para pagar. */
    @Transactional
    public AgendamentoView vincularCobranca(Long agendamentoId, CobrancaSinal cobranca) {
        Agendamento agendamento = buscarEntidade(agendamentoId);
        agendamento.setAsaasPaymentId(cobranca.cobrancaExternaId());
        agendamento.setPixPayload(cobranca.payload());

        AgendamentoView view = AgendamentoView.de(agendamento);
        if (agendamento.getStatus().aguardaSinal()) {
            publicar(AgendamentoEvento.Tipo.AGUARDANDO_PAGAMENTO, view);
        }
        return view;
    }

    /** O gateway recusou a cobranca: devolve o horario na hora, sem avisar o cliente de uma reserva que nao vai existir. */
    @Transactional
    public void liberarPorFalhaNaCobranca(Long agendamentoId) {
        buscarEntidade(agendamentoId).liberarPorFalhaNaCobranca();
    }

    // ---- pagamento confirmado pelo gateway (webhook) ----

    /** O que aconteceu com um agendamento quando o gateway avisou que o sinal foi pago. */
    public enum ResultadoPagamento {
        /** Estava esperando e agora esta confirmado. */
        CONFIRMADO,
        /** Ja estava confirmado (o gateway reenvia o mesmo evento): nada a fazer. */
        JA_ESTAVA_CONFIRMADO,
        /** Expirou antes de o pagamento chegar: o horario pode ou nao estar livre ainda. */
        HORARIO_LIBERADO,
        /** O agendamento foi cancelado: o dinheiro entrou por um horario que nao existe mais. */
        CANCELADO,
        /** Nenhum agendamento com essa cobranca. */
        DESCONHECIDO
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> buscarTenantDaCobranca(String cobrancaExternaId) {
        return agendamentoRepository.findByAsaasPaymentIdComTenant(cobrancaExternaId).map(Agendamento::getTenant);
    }

    /**
     * Registra o pagamento do sinal. Idempotente: webhooks repetem, e o mesmo evento processado de novo
     * nao pode dar erro, mudar o status nem avisar o cliente duas vezes.
     */
    @Transactional
    public ResultadoPagamento registrarPagamento(String cobrancaExternaId, Instant agora) {
        Optional<Agendamento> encontrado = agendamentoRepository.findWithLockByAsaasPaymentId(cobrancaExternaId);
        if (encontrado.isEmpty()) {
            return ResultadoPagamento.DESCONHECIDO;
        }
        Agendamento agendamento = encontrado.get();

        return switch (agendamento.getStatus()) {
            case AGUARDANDO_PAGAMENTO, AGUARDANDO_CONFIRMACAO -> {
                agendamento.confirmarSinal(agora);
                publicar(AgendamentoEvento.Tipo.CONFIRMADO, AgendamentoView.de(agendamento));
                yield ResultadoPagamento.CONFIRMADO;
            }
            case CONFIRMADO, CONCLUIDO, FALTOU -> ResultadoPagamento.JA_ESTAVA_CONFIRMADO;
            case EXPIRADO -> ResultadoPagamento.HORARIO_LIBERADO;
            case CANCELADO_CLIENTE, CANCELADO_NEGOCIO -> ResultadoPagamento.CANCELADO;
        };
    }

    /**
     * Pagamento que chegou depois de o prazo acabar: tenta trazer o agendamento de volta. Se outra pessoa ja
     * tomou o horario, a constraint do banco recusa e sobe ConflitoDeHorarioException - por isso esta
     * tentativa e uma transacao PROPRIA, separada de registrarPagamento (uma violacao de constraint deixa a
     * transacao inutilizavel, e o chamador ainda precisa decidir o que fazer com o dinheiro recebido).
     */
    @Transactional
    public AgendamentoView reativarPorPagamentoTardio(String cobrancaExternaId, Instant agora) {
        Agendamento agendamento = agendamentoRepository.findWithLockByAsaasPaymentId(cobrancaExternaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Agendamento não encontrado para a cobrança."));
        agendamento.reativarPorPagamentoTardio(agora);

        AgendamentoView view = AgendamentoView.de(gravar(agendamento));
        publicar(AgendamentoEvento.Tipo.CONFIRMADO, view);
        return view;
    }

    // ---- acoes do painel ----

    /** O dono confirma que recebeu o sinal, pelo painel. Idempotente; se a reserva ja expirou, volta so se o horario estiver livre (409). */
    @Transactional
    public AgendamentoView confirmarSinalManual(Long tenantId, Long agendamentoId, Instant agora) {
        return confirmarPeloDono(buscarEntidadeDoTenantParaAtualizar(tenantId, agendamentoId), agora);
    }

    /** O dono nao achou o Pix no extrato, pelo painel: cancela e devolve o horario. So vale depois de o cliente avisar que pagou. */
    @Transactional
    public AgendamentoView recusarPagamentoManual(Long tenantId, Long agendamentoId) {
        return recusarPagamento(buscarEntidadeDoTenantParaAtualizar(tenantId, agendamentoId));
    }

    @Transactional
    public AgendamentoView cancelarPeloNegocio(Long tenantId, Long agendamentoId) {
        Agendamento agendamento = buscarEntidadeDoTenantParaAtualizar(tenantId, agendamentoId);
        agendamento.cancelarPeloNegocio();

        AgendamentoView view = AgendamentoView.de(agendamento);
        publicar(AgendamentoEvento.Tipo.CANCELADO_PELO_NEGOCIO, view);
        return view;
    }

    @Transactional
    public AgendamentoView concluir(Long tenantId, Long agendamentoId, Instant agora) {
        Agendamento agendamento = buscarEntidadeDoTenantParaAtualizar(tenantId, agendamentoId);
        agendamento.concluir(agora);
        return AgendamentoView.de(agendamento);
    }

    @Transactional
    public AgendamentoView marcarFalta(Long tenantId, Long agendamentoId, Instant agora) {
        Agendamento agendamento = buscarEntidadeDoTenantParaAtualizar(tenantId, agendamentoId);
        agendamento.marcarFalta(agora);
        return AgendamentoView.de(agendamento);
    }

    // ---- Pix proprio: o cliente avisa que pagou e o dono confere e confirma ----

    /**
     * O cliente avisa que pagou (botao "Ja paguei"): o horario passa a ficar segurado por mais tempo e o dono recebe,
     * no WhatsApp, o link para conferir e confirmar. Idempotente - tocar duas vezes nao manda dois pedidos ao dono.
     * Vale tambem depois de a reserva expirar (o cliente pagou, so demorou): o horario volta se ainda estiver livre;
     * se nao, sobe ConflitoDeHorarioException e cabe a AgendamentoPagamentoService avisar o dono do dinheiro perdido.
     */
    @Transactional
    public AgendamentoView informarPagamento(String slug, UUID codigoPublico, Instant agora) {
        Tenant tenant = tenantService.buscarPorSlug(slug);
        Agendamento agendamento = agendamentoRepository
                .findWithLockByCodigoPublicoAndTenantId(codigoPublico, tenant.getId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Agendamento não encontrado."));

        TokenConfirmacao.Gerado token = TokenConfirmacao.gerar();
        if (!agendamento.informarPagamento(agora, PRAZO_CONFIRMACAO_MANUAL, token.hash())) {
            return AgendamentoView.de(agendamento);
        }
        AgendamentoView view = AgendamentoView.de(gravar(agendamento, MENSAGEM_HORARIO_OCUPADO_NO_AVISO));
        publicar(AgendamentoEvento.Tipo.PAGAMENTO_INFORMADO, view, token.valor());
        return view;
    }

    /** Lado publico do link do dono: mostra o que esta sendo confirmado. So o dono tem o token (ver TokenConfirmacao). */
    @Transactional(readOnly = true)
    public AgendamentoView buscarPorTokenDoDono(String token) {
        return agendamentoRepository.findByTokenConfirmacaoHash(hashDoToken(token))
                .map(AgendamentoView::de)
                .orElseThrow(this::linkDeConfirmacaoInvalido);
    }

    @Transactional
    public AgendamentoView confirmarPorToken(String token, Instant agora) {
        return confirmarPeloDono(buscarPorTokenParaAtualizar(token), agora);
    }

    @Transactional
    public AgendamentoView recusarPorToken(String token) {
        return recusarPagamento(buscarPorTokenParaAtualizar(token));
    }

    private Agendamento buscarPorTokenParaAtualizar(String token) {
        return agendamentoRepository.findWithLockByTokenConfirmacaoHash(hashDoToken(token))
                .orElseThrow(this::linkDeConfirmacaoInvalido);
    }

    private String hashDoToken(String token) {
        if (!TokenConfirmacao.formatoValido(token)) {
            throw linkDeConfirmacaoInvalido();
        }
        return TokenConfirmacao.hash(token);
    }

    private EntidadeNaoEncontradaException linkDeConfirmacaoInvalido() {
        return new EntidadeNaoEncontradaException("Link inválido ou substituído por um mais recente.");
    }

    private AgendamentoView confirmarPeloDono(Agendamento agendamento, Instant agora) {
        if (!agendamento.confirmarPeloDono(agora)) {
            return AgendamentoView.de(agendamento);
        }
        AgendamentoView view = AgendamentoView.de(gravar(agendamento, MENSAGEM_HORARIO_OCUPADO_NA_CONFIRMACAO));
        publicar(AgendamentoEvento.Tipo.CONFIRMADO, view);
        return view;
    }

    private AgendamentoView recusarPagamento(Agendamento agendamento) {
        boolean recusou = agendamento.recusarPagamento();
        AgendamentoView view = AgendamentoView.de(agendamento);
        if (recusou) {
            publicar(AgendamentoEvento.Tipo.PAGAMENTO_NAO_LOCALIZADO, view);
        }
        return view;
    }

    // ---- expiracao ----

    /**
     * Expira em lote quem esperava e passou do prazo, devolvendo o horario para a agenda.
     * Idempotente e segura com varias instancias (ver findVencidosParaExpirar).
     *
     * @return quantos expiraram nesta execucao
     */
    @Transactional
    public int expirarVencidos(Instant agora) {
        int expirados = 0;
        for (Agendamento agendamento : agendamentoRepository
                .findVencidosParaExpirar(AgendamentoStatus.EM_ESPERA, agora, Limit.of(LOTE_DE_EXPIRACAO))) {
            if (agendamento.expirarSeVencido(agora)) {
                publicar(AgendamentoEvento.Tipo.EXPIRADO, AgendamentoView.de(agendamento));
                expirados++;
            }
        }
        return expirados;
    }

    // ---- consultas ----

    @Transactional(readOnly = true)
    public AgendamentoView buscarDoTenant(Long tenantId, Long agendamentoId) {
        return AgendamentoView.de(buscarEntidadeDoTenant(tenantId, agendamentoId));
    }

    /** O agendamento de uma cobranca (para montar o aviso de dinheiro sem horario); vazio se a cobranca nao e de nenhum. */
    @Transactional(readOnly = true)
    public Optional<AgendamentoView> buscarPorCobrancaExterna(String cobrancaExternaId) {
        return agendamentoRepository.findByAsaasPaymentId(cobrancaExternaId).map(AgendamentoView::de);
    }

    /** Consulta publica por codigo aleatorio, sempre dentro do negocio do link. */
    @Transactional(readOnly = true)
    public AgendamentoView buscarPorCodigoPublico(String slug, UUID codigoPublico) {
        return agendamentoRepository.findByCodigoPublicoAndTenantSlug(codigoPublico, slug)
                .map(AgendamentoView::de)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Agendamento não encontrado."));
    }

    /** Agenda do painel entre duas datas (inclusivas, no fuso do negocio), de um profissional ou de todos. */
    @Transactional(readOnly = true)
    public List<AgendamentoView> listarAgenda(Long tenantId, Long profissionalId, LocalDate de, LocalDate ate) {
        ZoneId fuso = ZoneId.of(tenantService.buscarPorId(tenantId).getFusoHorario());
        return agendamentoRepository
                .findAgenda(tenantId, profissionalId, de.atStartOfDay(fuso).toInstant(), ate.plusDays(1).atStartOfDay(fuso).toInstant())
                .stream()
                .map(AgendamentoView::de)
                .toList();
    }

    private Agendamento buscarEntidade(Long agendamentoId) {
        return agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Agendamento não encontrado (id=" + agendamentoId + ")."));
    }

    /** Para quem vai mudar o estado: com lock (ver AgendamentoRepository). Nao usar em transacao readOnly. */
    private Agendamento buscarEntidadeDoTenantParaAtualizar(Long tenantId, Long agendamentoId) {
        return agendamentoRepository.findWithLockByIdAndTenantId(agendamentoId, tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Agendamento não encontrado (id=" + agendamentoId + ")."));
    }

    private Agendamento buscarEntidadeDoTenant(Long tenantId, Long agendamentoId) {
        return agendamentoRepository.findByIdAndTenantId(agendamentoId, tenantId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Agendamento não encontrado (id=" + agendamentoId + ")."));
    }

    private void publicar(AgendamentoEvento.Tipo tipo, AgendamentoView view) {
        eventPublisher.publishEvent(AgendamentoEvento.de(tipo, view));
    }

    private void publicar(AgendamentoEvento.Tipo tipo, AgendamentoView view, String tokenConfirmacao) {
        eventPublisher.publishEvent(AgendamentoEvento.de(tipo, view, tokenConfirmacao));
    }

}
