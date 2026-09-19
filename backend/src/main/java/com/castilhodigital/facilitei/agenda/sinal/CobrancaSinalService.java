package com.castilhodigital.facilitei.agenda.sinal;

import com.castilhodigital.facilitei.tenant.Tenant;
import java.util.Optional;

/**
 * Como o sinal de um agendamento e cobrado - uma "estrategia" escolhida por
 * negocio (ver CobrancaSinalResolver). Hoje a Asaas na conta do proprio
 * negocio; o Pix com a chave propria do dono, com confirmacao manual, entra
 * como outra implementacao sem o agendamento saber a diferenca.
 */
public interface CobrancaSinalService {

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

}
