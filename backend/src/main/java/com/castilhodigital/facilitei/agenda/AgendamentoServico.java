package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.catalog.ServiceOffering;
import com.castilhodigital.facilitei.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um servico dentro de um agendamento. Nome, duracao e preco sao uma COPIA do
 * que valia na hora da reserva: alterar o preco de "Corte" depois nao pode
 * mudar o historico do cliente nem o faturamento dos relatorios. A referencia
 * ao servico original (servico) e so para navegacao e fica nula se ele for excluido.
 */
@Entity
@Table(name = "agendamento_servicos")
@Getter
@Setter
@NoArgsConstructor
public class AgendamentoServico extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private ServiceOffering servico;

    /** Posicao na sequencia do atendimento (1, 2, ...). */
    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false)
    private String nome;

    @Column(name = "duracao_min", nullable = false)
    private int duracaoMin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    public static AgendamentoServico copiaDe(ServiceOffering servico, int ordem) {
        AgendamentoServico item = new AgendamentoServico();
        item.setServico(servico);
        item.setOrdem(ordem);
        item.setNome(servico.getNome());
        item.setDuracaoMin(servico.getDuracaoMin());
        item.setPreco(servico.getPreco());
        return item;
    }

}
