package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.professional.Profissional;
import com.castilhodigital.facilitei.tenant.Tenant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um cliente com um profissional, de inicio a fim, para um ou mais servicos.
 * Substitui o par Slot + Booking da agenda antiga: em vez de um horario
 * pre-gerado que "muda de status", o agendamento e um intervalo de tempo real,
 * e quem impede dois deles de se sobreporem para o mesmo profissional e a
 * constraint de exclusao do banco (V16), nao codigo Java.
 */
@Entity
@Table(name = "agendamentos")
@Getter
@Setter
@NoArgsConstructor
public class Agendamento extends BaseEntity {

    /** Vai em links publicos (pagina de pagamento, confirmacao pelo dono); o id sequencial nunca sai na API publica. */
    @Column(name = "codigo_publico", nullable = false, updatable = false)
    private UUID codigoPublico = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    /** Preenchido so quando o cliente esta logado no portal; nulo para reserva pelo link direto ou criada no painel. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "cliente_nome", nullable = false)
    private String clienteNome;

    @Column(name = "cliente_telefone", nullable = false)
    private String clienteTelefone;

    /** Opcional: so e necessario quando o sinal e cobrado por um gateway que exige documento (Asaas). */
    @Column(name = "cliente_cpf_cnpj")
    private String clienteCpfCnpj;

    @Column(nullable = false)
    private Instant inicio;

    /** Fim = inicio + soma das duracoes dos servicos. Guardado (nao recalculado) para a constraint do banco enxergar o intervalo. */
    @Column(nullable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgendamentoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgendamentoOrigem origem;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_sinal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorSinal = BigDecimal.ZERO;

    /** Obrigatorio enquanto o status for AGUARDANDO_* (o banco exige); depois disso perde o sentido. */
    @Column(name = "expira_em")
    private Instant expiraEm;

    /** Por qual meio o sinal deste agendamento foi cobrado; nulo se nao ha sinal. Guardado porque o dono pode trocar de meio depois. */
    @Enumerated(EnumType.STRING)
    @Column(name = "meio_cobranca", length = 20)
    private MeioCobranca meioCobranca;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    /** Pix "copia e cola" do sinal, guardado para a tela de pagamento sobreviver a um F5 sem chamar o gateway de novo. */
    @Column(name = "pix_payload", columnDefinition = "TEXT")
    private String pixPayload;

    @Column(name = "sinal_confirmado_em")
    private Instant sinalConfirmadoEm;

    /** Pix proprio: quando o cliente disse "ja paguei". O dono ainda precisa conferir no banco e confirmar. */
    @Column(name = "pagamento_informado_em")
    private Instant pagamentoInformadoEm;

    /** SHA-256 do token do link enviado ao dono (ver TokenConfirmacao); o token em si nunca e gravado. */
    @Column(name = "token_confirmacao_hash", length = 64)
    private String tokenConfirmacaoHash;

    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<AgendamentoServico> servicos = new ArrayList<>();

    /** Mantem os dois lados da associacao coerentes; e a unica forma prevista de adicionar servico ao agendamento. */
    public void adicionarServico(AgendamentoServico item) {
        item.setAgendamento(this);
        servicos.add(item);
    }

    // ---- transicoes de estado: as regras do ciclo de vida moram aqui, nao espalhadas pelos services ----

    /**
     * O sinal foi pago (webhook do gateway) ou o dono confirmou o recebimento.
     * Idempotente para quem ja passou dessa etapa: gateways reenviam o mesmo
     * webhook, e processar de novo nao pode dar erro nem "voltar" o status.
     */
    public void confirmarSinal(Instant agora) {
        if (status == AgendamentoStatus.CONFIRMADO || status == AgendamentoStatus.CONCLUIDO
                || status == AgendamentoStatus.FALTOU) {
            return;
        }
        if (!status.aguardaSinal()) {
            throw new RegraDeNegocioException("Este agendamento não está aguardando pagamento.");
        }
        status = AgendamentoStatus.CONFIRMADO;
        sinalConfirmadoEm = agora;
        expiraEm = null;
    }

    /**
     * Pagamento que chegou DEPOIS de o prazo acabar. Ressuscitar a reserva so e
     * seguro se ninguem tomou o horario nesse meio tempo - e quem decide isso e a
     * constraint de exclusao do banco quando este estado for gravado (ver
     * AgendamentoService.confirmarSinalDoGateway), nao uma checagem aqui.
     */
    public void reativarPorPagamentoTardio(Instant agora) {
        if (status != AgendamentoStatus.EXPIRADO) {
            throw new RegraDeNegocioException("Só um agendamento expirado pode ser reativado por pagamento tardio.");
        }
        status = AgendamentoStatus.CONFIRMADO;
        sinalConfirmadoEm = agora;
        expiraEm = null;
    }

    /**
     * O cliente avisou que pagou o sinal (Pix proprio): o horario segue segurado, agora por mais tempo, ate o
     * dono conferir o extrato e confirmar. Tambem vale para quem avisa DEPOIS do prazo (a reserva expirou, mas o
     * cliente pagou) - o horario so volta se ainda estiver livre, e quem decide isso e a constraint do banco quando
     * este estado e gravado (o chamador trata ConflitoDeHorarioException).
     *
     * @param tokenHash hash do segredo do link enviado ao dono (ver TokenConfirmacao)
     * @return true se mudou de estado agora (quem chama avisa o dono); false se ja estava esperando ou confirmado -
     *         o cliente que toca no botao duas vezes nao pode gerar dois pedidos ao dono
     */
    public boolean informarPagamento(Instant agora, Duration prazoDeConfirmacao, String tokenHash) {
        if (meioCobranca == null || !meioCobranca.confirmacaoManual()) {
            throw new RegraDeNegocioException("Este pagamento é confirmado automaticamente, não é preciso avisar.");
        }
        switch (status) {
            case AGUARDANDO_CONFIRMACAO, CONFIRMADO, CONCLUIDO, FALTOU -> {
                return false;
            }
            case CANCELADO_CLIENTE, CANCELADO_NEGOCIO ->
                    throw new RegraDeNegocioException("Este agendamento foi cancelado.");
            case AGUARDANDO_PAGAMENTO, EXPIRADO -> {
                if (!agora.isBefore(inicio)) {
                    throw new RegraDeNegocioException("O horário deste agendamento já passou.");
                }
                Instant limite = agora.plus(prazoDeConfirmacao);
                status = AgendamentoStatus.AGUARDANDO_CONFIRMACAO;
                pagamentoInformadoEm = agora;
                expiraEm = limite.isAfter(inicio) ? inicio : limite;   // segurar o horario alem da hora marcada nao faz sentido
                tokenConfirmacaoHash = tokenHash;
                return true;
            }
        }
        throw new IllegalStateException("Status nao tratado: " + status);
    }

    /**
     * O dono confirma que o Pix caiu (pelo link do WhatsApp ou pelo painel). Alem de quem esta esperando, aceita quem
     * ja expirou: o dono atesta o recebimento e a reserva volta se o horario ainda estiver livre (constraint do banco).
     *
     * @return true se confirmou agora; false se ja estava confirmado (o dono clicou duas vezes)
     */
    public boolean confirmarPeloDono(Instant agora) {
        switch (status) {
            case CONFIRMADO, CONCLUIDO, FALTOU -> {
                return false;
            }
            case AGUARDANDO_PAGAMENTO, AGUARDANDO_CONFIRMACAO -> {
                confirmarSinal(agora);
                return true;
            }
            case EXPIRADO -> {
                reativarPorPagamentoTardio(agora);
                return true;
            }
            case CANCELADO_CLIENTE, CANCELADO_NEGOCIO ->
                    throw new RegraDeNegocioException("Este agendamento foi cancelado, o horário não pode mais ser confirmado.");
        }
        throw new IllegalStateException("Status nao tratado: " + status);
    }

    /**
     * O dono nao encontrou o Pix no extrato: cancela e devolve o horario a agenda.
     *
     * @return true se cancelou agora; false se o horario ja estava liberado (expirado ou cancelado)
     */
    public boolean recusarPagamento() {
        switch (status) {
            case AGUARDANDO_CONFIRMACAO -> {
                status = AgendamentoStatus.CANCELADO_NEGOCIO;
                expiraEm = null;
                return true;
            }
            case EXPIRADO, CANCELADO_CLIENTE, CANCELADO_NEGOCIO -> {
                return false;
            }
            case AGUARDANDO_PAGAMENTO ->
                    throw new RegraDeNegocioException("O cliente ainda não avisou que pagou. Para liberar o horário, cancele o agendamento.");
            case CONFIRMADO, CONCLUIDO, FALTOU ->
                    throw new RegraDeNegocioException("Este pagamento já foi confirmado. Para desfazer, cancele o agendamento no painel.");
        }
        throw new IllegalStateException("Status nao tratado: " + status);
    }

    /** Cancelamento feito pelo dono/funcionario; vale enquanto o atendimento ainda nao aconteceu. */
    public void cancelarPeloNegocio() {
        if (!status.aguardaSinal() && status != AgendamentoStatus.CONFIRMADO) {
            throw new RegraDeNegocioException("Este agendamento não pode mais ser cancelado.");
        }
        status = AgendamentoStatus.CANCELADO_NEGOCIO;
        expiraEm = null;
    }

    public void concluir(Instant agora) {
        exigirConfirmadoEJaIniciado(agora, "concluído");
        status = AgendamentoStatus.CONCLUIDO;
    }

    public void marcarFalta(Instant agora) {
        exigirConfirmadoEJaIniciado(agora, "marcado como falta");
        status = AgendamentoStatus.FALTOU;
    }

    /**
     * Passa para EXPIRADO se estava esperando e o prazo ja acabou.
     *
     * @return true se expirou agora (quem chama publica o evento e avisa o cliente)
     */
    public boolean expirarSeVencido(Instant agora) {
        if (status.aguardaSinal() && expiraEm != null && !expiraEm.isAfter(agora)) {
            status = AgendamentoStatus.EXPIRADO;
            return true;
        }
        return false;
    }

    /** A cobranca do sinal nao pode ser criada no gateway: devolve o horario para a agenda na hora, sem esperar o prazo. */
    public void liberarPorFalhaNaCobranca() {
        if (status.aguardaSinal()) {
            status = AgendamentoStatus.EXPIRADO;
        }
    }

    private void exigirConfirmadoEJaIniciado(Instant agora, String acao) {
        if (status != AgendamentoStatus.CONFIRMADO) {
            throw new RegraDeNegocioException("Só um agendamento confirmado pode ser " + acao + ".");
        }
        if (agora.isBefore(inicio)) {
            throw new RegraDeNegocioException("O atendimento ainda não começou.");
        }
    }

}
