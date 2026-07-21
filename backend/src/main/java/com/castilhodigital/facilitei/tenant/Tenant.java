package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um negocio (dono da agenda) cadastrado na plataforma. E o "tenant" que
 * isola os dados de cada cliente do SaaS (services, slots, bookings, users).
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    /** Identificador publico e amigavel usado na URL da pagina de agendamento (ex.: /b/barbearia-do-ze). */
    @Column(nullable = false, unique = true)
    private String slug;

    /**
     * Id da wallet/subconta Asaas do tenant, usado no futuro para split de
     * pagamento (repassar o valor do sinal diretamente ao dono do negocio).
     * No MVP fica nulo: a cobranca cai na conta Asaas da plataforma e o
     * repasse e feito manualmente (limitacao conhecida, documentada no README).
     */
    @Column(name = "asaas_wallet_id")
    private String asaasWalletId;

    @Column(name = "horario_abertura", nullable = false)
    private LocalTime horarioAbertura;

    @Column(name = "horario_fechamento", nullable = false)
    private LocalTime horarioFechamento;

}
