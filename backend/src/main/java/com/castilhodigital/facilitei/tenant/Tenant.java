package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.common.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    /**
     * Chave da API Asaas da PROPRIA conta do tenant (modelo "traga sua
     * propria conta de pagamento" - BYOPP): a cobranca Pix do sinal e criada
     * direto na conta do negocio, nao na da plataforma - diferente do
     * asaasWalletId acima, que era pensado para um modelo de split via
     * subconta/marketplace nunca implementado. Cifrada em repouso (ver
     * common.crypto.EncryptedStringConverter).
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "asaas_api_key", columnDefinition = "TEXT")
    private String asaasApiKey;

    /**
     * Segredo gerado pela PROPRIA plataforma (nao escolhido pelo tenant) ao
     * configurar a chave acima, usado para autenticar o webhook Asaas deste
     * tenant especificamente - ver AsaasWebhookController, que identifica o
     * tenant pelo pagamento recebido antes de validar o token.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "asaas_webhook_token", columnDefinition = "TEXT")
    private String asaasWebhookToken;

    @Column(name = "horario_abertura", nullable = false)
    private LocalTime horarioAbertura;

    @Column(name = "horario_fechamento", nullable = false)
    private LocalTime horarioFechamento;

}
