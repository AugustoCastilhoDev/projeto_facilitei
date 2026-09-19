package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.billing.AssinaturaStatus;
import com.castilhodigital.facilitei.billing.Plano;
import com.castilhodigital.facilitei.common.BaseEntity;
import com.castilhodigital.facilitei.common.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
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

    /** CPF/CNPJ do negocio, usado para gerar a cobranca Pix da assinatura (a Asaas exige um cpfCnpj por customer). */
    @Column(name = "cpf_cnpj", length = 20)
    private String cpfCnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plano plano = Plano.BASICO;

    /**
     * Status da assinatura do tenant com a PROPRIA plataforma (ver pacote
     * billing) - nao confundir com o pagamento do sinal (BYOPP). Tenants
     * criados antes desta feature entram como ATIVA (grandfathered, ver
     * migration V11) para nao bloquear uso ja existente retroativamente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assinatura_status", nullable = false, length = 20)
    private AssinaturaStatus assinaturaStatus = AssinaturaStatus.ATIVA;

    @Column(name = "trial_ate")
    private LocalDate trialAte;

    @Column(name = "proxima_cobranca_em")
    private LocalDate proximaCobrancaEm;

    /** Campos do diretorio publico (portal do cliente) - todos opcionais, so preenchidos se o tenant optar por aparecer la. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CategoriaNegocio categoria;

    @Column(length = 100)
    private String cidade;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /**
     * Opt-in explicito para aparecer no diretorio (GET /api/public/directory).
     * Default false: nenhum tenant existente aparece la ate escolher - o
     * diretorio e complemento ao link direto /agendar/:slug, nunca substituto.
     */
    @Column(name = "visivel_no_diretorio", nullable = false)
    private boolean visivelNoDiretorio = false;

    /** Fuso IANA do negocio (ex.: America/Manaus). Os horarios de trabalho sao "hora de parede" neste fuso. */
    @Column(name = "fuso_horario", nullable = false, length = 50)
    private String fusoHorario = "America/Sao_Paulo";

    /** Passo da grade de horarios oferecidos ao cliente (minutos): 30 gera 09:00, 09:30, 10:00... */
    @Column(name = "intervalo_agenda_min", nullable = false)
    private int intervaloAgendaMin = 30;

    /** Antecedencia minima para marcar (minutos): impede reservar "daqui a 5 minutos". */
    @Column(name = "antecedencia_minima_min", nullable = false)
    private int antecedenciaMinimaMin = 60;

    /** Ate quantos dias a frente a agenda aceita marcacao. */
    @Column(name = "horizonte_dias", nullable = false)
    private int horizonteDias = 60;

}
