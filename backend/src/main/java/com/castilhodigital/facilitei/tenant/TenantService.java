package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.billing.AssinaturaStatus;
import com.castilhodigital.facilitei.billing.Plano;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS_DE_TRIAL = 30;

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant registrar(String nome, String slug, String cpfCnpj, Plano plano) {
        if (tenantRepository.existsBySlug(slug)) {
            throw new RegraDeNegocioException("Ja existe um tenant com o slug '" + slug + "'.");
        }

        Tenant tenant = new Tenant();
        tenant.setNome(nome);
        tenant.setSlug(slug);
        tenant.setCpfCnpj(cpfCnpj);
        tenant.setPlano(plano);
        tenant.setAssinaturaStatus(AssinaturaStatus.TRIAL);
        tenant.setTrialAte(LocalDate.now(ZONE_ID).plusDays(DIAS_DE_TRIAL));
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant buscarPorSlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Tenant nao encontrado para o slug '" + slug + "'."));
    }

    @Transactional(readOnly = true)
    public Tenant buscarPorId(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Tenant nao encontrado (id=" + id + ")."));
    }

    /**
     * Salva a chave Asaas da PROPRIA conta do tenant (modelo BYOPP). Na
     * primeira configuracao, gera tambem o token de webhook deste tenant -
     * gerado uma unica vez e nunca trocado depois, para nao invalidar um
     * webhook ja configurado do lado da Asaas.
     */
    @Transactional
    public Tenant configurarAsaas(Long tenantId, String apiKey) {
        Tenant tenant = buscarPorId(tenantId);
        tenant.setAsaasApiKey(apiKey);
        if (tenant.getAsaasWebhookToken() == null) {
            tenant.setAsaasWebhookToken(UUID.randomUUID().toString());
        }
        return tenant;
    }

}
