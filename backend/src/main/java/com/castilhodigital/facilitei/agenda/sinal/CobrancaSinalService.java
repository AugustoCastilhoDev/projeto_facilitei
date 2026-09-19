package com.castilhodigital.facilitei.agenda.sinal;

import com.castilhodigital.facilitei.tenant.Tenant;
import java.util.Optional;

/**
 * Como o sinal de um agendamento e cobrado - uma "estrategia" escolhida por
 * negocio (ver CobrancaSinalResolver): a Asaas na conta do proprio negocio
 * (confirmacao automatica) ou o Pix com a chave propria do dono (confirmacao
 * manual). O agendamento so conhece esta interface.
 */
public interface CobrancaSinalService {

    /** Identifica o meio; e o que fica gravado no agendamento para ele seguir as regras do meio com que nasceu. */
    MeioCobranca meio();

    /** Este negocio esta configurado para receber o sinal por este meio? */
    boolean atende(Tenant tenant);

    /**
     * Confere, ANTES de o horario ser segurado, se a cobranca poderia ser criada
     * (ex.: o gateway exige CPF/CNPJ). Falhar aqui e barato; falhar depois de
     * segurar o horario obriga a devolve-lo.
     *
     * @throws com.castilhodigital.facilitei.common.exception.RegraDeNegocioException se faltar algum dado
     */
    void validar(SolicitacaoCobranca solicitacao);

    /** Cria a cobranca no meio de pagamento. Chamado FORA de transacao de banco (e uma chamada de rede). */
    CobrancaSinal criar(Tenant tenant, SolicitacaoCobranca solicitacao);

    /** Reobtem a imagem do QR de uma cobranca pendente (nao e persistida); vazio se este meio nao tem como. */
    Optional<String> buscarQrCodeBase64(Tenant tenant, String cobrancaExternaId);

    /**
     * Cancela uma cobranca ainda nao paga (o horario foi liberado ou o dono cancelou), para ninguem pagar por
     * um horario que nao e mais dele. Melhor esforco: quem chama nao deve deixar isso desfazer o que ja aconteceu.
     */
    void cancelar(Tenant tenant, String cobrancaExternaId);

}
