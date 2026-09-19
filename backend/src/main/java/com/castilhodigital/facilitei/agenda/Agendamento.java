package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.common.BaseEntity;
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

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    /** Pix "copia e cola" do sinal, guardado para a tela de pagamento sobreviver a um F5 sem chamar o gateway de novo. */
    @Column(name = "pix_payload", columnDefinition = "TEXT")
    private String pixPayload;

    @Column(name = "sinal_confirmado_em")
    private Instant sinalConfirmadoEm;

    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<AgendamentoServico> servicos = new ArrayList<>();

    /** Mantem os dois lados da associacao coerentes; e a unica forma prevista de adicionar servico ao agendamento. */
    public void adicionarServico(AgendamentoServico item) {
        item.setAgendamento(this);
        servicos.add(item);
    }

}
