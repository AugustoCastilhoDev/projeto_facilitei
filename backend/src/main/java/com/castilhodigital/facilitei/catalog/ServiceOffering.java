package com.castilhodigital.facilitei.catalog;

import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.tenant.Tenant;
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
 * Servico oferecido por um tenant (ex.: "Corte de cabelo", 30min, R$ 50).
 * Mapeia a tabela "services" do modelo original; a classe Java foi nomeada
 * ServiceOffering (em vez de "Service") para nao colidir com a anotacao
 * org.springframework.stereotype.Service e para deixar os nomes da camada
 * de servico de dominio sem ambiguidade (ex.: ServiceOfferingService).
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class ServiceOffering extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String nome;

    @Column(name = "duracao_min", nullable = false)
    private Integer duracaoMin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    /** Percentual do preco cobrado como sinal via Pix no momento do agendamento (0 a 100). */
    @Column(name = "sinal_percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal sinalPercentual;

    /** Soft-disable: servicos inativos somem da agenda publica mas preservam o historico de bookings. */
    @Column(nullable = false)
    private boolean ativo = true;

}
