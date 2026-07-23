package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.scheduling.Slot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reserva feita por um cliente final para um slot. Relacao 1:1 com Slot
 * (um slot so pode ter uma reserva ativa por vez - reforcado por unique
 * constraint no banco).
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private Slot slot;

    @Column(name = "cliente_nome", nullable = false)
    private String clienteNome;

    @Column(name = "cliente_telefone", nullable = false)
    private String clienteTelefone;

    /**
     * Obrigatorio na API (ver CriarBookingRequest): a Asaas exige CPF/CNPJ
     * para gerar a cobranca, ainda que nao exija para criar o cliente em si.
     * Coluna fica nullable porque a constraint de negocio e aplicada na
     * validacao do request, nao no schema.
     */
    @Column(name = "cliente_cpf_cnpj")
    private String clienteCpfCnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private PaymentStatus statusPagamento = PaymentStatus.PENDENTE;

    /** Id da cobranca no Asaas, usado para casar o webhook com este booking. */
    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    /** Payload "copia e cola" do Pix, cacheado para nao chamar o Asaas de novo a cada exibicao da tela. */
    @Column(name = "asaas_pix_payload", columnDefinition = "TEXT")
    private String asaasPixPayload;

    /** Marcado manualmente pelo admin apos o horario do compromisso. Null = ainda nao marcado. */
    @Column(name = "compareceu")
    private Boolean compareceu;

}
