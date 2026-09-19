package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.sinal.MeioCobranca;
import com.castilhodigital.facilitei.pix.TipoChavePix;
import com.castilhodigital.facilitei.tenant.Tenant;

/**
 * Como o negocio recebe o sinal hoje.
 *
 * @param meioAtivo         o meio que as NOVAS reservas vao usar; nulo se nada foi configurado (o negocio so
 *                          consegue vender servico sem sinal)
 * @param asaasConfigurado  ha chave da Asaas cadastrada (o meio ativo e a Asaas, que tem precedencia sobre o Pix proprio)
 * @param pixProprio        a chave Pix cadastrada, se houver
 * @param telefoneDono      WhatsApp para onde vao os pedidos de confirmacao; nulo se ainda nao cadastrado
 */
public record RecebimentoResponse(MeioCobranca meioAtivo, boolean asaasConfigurado, PixProprio pixProprio,
                                  String telefoneDono) {

    public record PixProprio(TipoChavePix tipoChave, String chave, String nomeRecebedor, String cidadeRecebedor) {
    }

    public static RecebimentoResponse de(Tenant tenant, MeioCobranca meioAtivo) {
        PixProprio pix = tenant.getPixChave() == null ? null : new PixProprio(
                tenant.getPixTipoChave(), tenant.getPixChave(), tenant.getPixNomeRecebedor(), tenant.getPixCidadeRecebedor());
        boolean asaas = tenant.getAsaasApiKey() != null && !tenant.getAsaasApiKey().isBlank();
        return new RecebimentoResponse(meioAtivo, asaas, pix, tenant.getTelefoneDono());
    }

}
