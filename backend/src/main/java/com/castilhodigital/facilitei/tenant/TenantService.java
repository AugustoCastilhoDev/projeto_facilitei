package com.castilhodigital.facilitei.tenant;

import com.castilhodigital.facilitei.billing.AssinaturaStatus;
import com.castilhodigital.facilitei.billing.Plano;
import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.common.validation.CpfCnpjUtils;
import com.castilhodigital.facilitei.common.validation.TelefoneBrasil;
import com.castilhodigital.facilitei.pix.BrCode;
import com.castilhodigital.facilitei.pix.ChavePix;
import com.castilhodigital.facilitei.pix.TipoChavePix;
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
    private static final String CIDADE_PADRAO_DO_BR_CODE = "BRASIL";

    private final TenantRepository tenantRepository;

    @Transactional
    public Tenant registrar(String nome, String slug, String cpfCnpj, Plano plano) {
        if (tenantRepository.existsBySlug(slug)) {
            throw new RegraDeNegocioException("Ja existe um tenant com o slug '" + slug + "'.");
        }

        Tenant tenant = new Tenant();
        tenant.setNome(nome);
        tenant.setSlug(slug);
        tenant.setCpfCnpj(CpfCnpjUtils.somenteDigitos(cpfCnpj));
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

    /** Onde o dono recebe os pedidos de confirmacao do Pix e os avisos de pagamento tardio. */
    @Transactional
    public Tenant atualizarTelefoneDono(Long tenantId, String telefone) {
        Tenant tenant = buscarPorId(tenantId);
        tenant.setTelefoneDono(TelefoneBrasil.normalizar(telefone));
        return tenant;
    }

    /**
     * Configura o recebimento do sinal na chave Pix do proprio negocio. Exige o WhatsApp do dono ja
     * cadastrado: como o banco nao avisa que o Pix caiu, e por la que o dono e chamado para confirmar
     * cada pagamento - sem isso, todo agendamento com sinal ficaria esperando ate expirar.
     *
     * Nome e cidade do recebedor sao so informativos no BR Code (o banco do cliente mostra o titular
     * real da chave); na falta deles usa o nome do negocio e a cidade do diretorio.
     */
    @Transactional
    public Tenant configurarPixProprio(Long tenantId, TipoChavePix tipo, String chave, String nomeRecebedor,
                                        String cidadeRecebedor) {
        Tenant tenant = buscarPorId(tenantId);
        if (tenant.getTelefoneDono() == null) {
            throw new RegraDeNegocioException(
                    "Cadastre o WhatsApp do dono antes: é para lá que enviamos o pedido de confirmação de cada Pix recebido.");
        }
        String chaveNormalizada = ChavePix.normalizar(tipo, chave);
        String nome = BrCode.sanitizar(
                nomeRecebedor == null || nomeRecebedor.isBlank() ? tenant.getNome() : nomeRecebedor, BrCode.TAMANHO_MAXIMO_NOME);
        if (nome.isEmpty()) {
            throw new RegraDeNegocioException("Informe o nome do recebedor com letras ou números.");
        }
        String cidadeBase = cidadeRecebedor == null || cidadeRecebedor.isBlank() ? tenant.getCidade() : cidadeRecebedor;
        String cidade = BrCode.sanitizar(cidadeBase, BrCode.TAMANHO_MAXIMO_CIDADE);

        tenant.setPixTipoChave(tipo);
        tenant.setPixChave(chaveNormalizada);
        tenant.setPixNomeRecebedor(nome);
        tenant.setPixCidadeRecebedor(cidade.isEmpty() ? CIDADE_PADRAO_DO_BR_CODE : cidade);
        return tenant;
    }

    /** Agendamentos ja criados seguem valendo: o Pix deles ja foi gerado e o meio esta gravado em cada um. */
    @Transactional
    public Tenant removerPixProprio(Long tenantId) {
        Tenant tenant = buscarPorId(tenantId);
        tenant.setPixTipoChave(null);
        tenant.setPixChave(null);
        tenant.setPixNomeRecebedor(null);
        tenant.setPixCidadeRecebedor(null);
        return tenant;
    }

    /**
     * O tenant so pode aparecer no diretorio publico (visivelNoDiretorio =
     * true) com categoria e cidade ja preenchidas - sem isso a listagem
     * ficaria com cards vazios/inuteis para o cliente final escolher.
     */
    @Transactional
    public Tenant atualizarDiretorio(Long tenantId, CategoriaNegocio categoria, String cidade, String descricao,
                                      boolean visivelNoDiretorio) {
        Tenant tenant = buscarPorId(tenantId);
        if (visivelNoDiretorio && (categoria == null || cidade == null || cidade.isBlank())) {
            throw new RegraDeNegocioException("Categoria e cidade sao obrigatorias para aparecer no diretorio.");
        }
        tenant.setCategoria(categoria);
        tenant.setCidade(cidade);
        tenant.setDescricao(descricao);
        tenant.setVisivelNoDiretorio(visivelNoDiretorio);
        return tenant;
    }

}
