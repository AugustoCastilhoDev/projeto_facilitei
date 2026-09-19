package com.castilhodigital.facilitei.agenda.sinal;

import com.castilhodigital.facilitei.pix.BrCode;
import com.castilhodigital.facilitei.pix.ReferenciaPix;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sinal recebido direto na chave Pix do dono, sem gateway nem conta a abrir. A plataforma so monta o
 * "copia e cola" (BR Code estatico com o valor do sinal); como o banco nao avisa ninguem que o Pix caiu,
 * quem confirma e o dono - o agendamento espera essa confirmacao (AGUARDANDO_CONFIRMACAO).
 *
 * Fica DEPOIS da Asaas na ordem de escolha: quem tem os dois configurados ganha confirmacao automatica.
 */
@Component
@Order(2)
class PixProprioCobrancaSinalService implements CobrancaSinalService {

    @Override
    public MeioCobranca meio() {
        return MeioCobranca.PIX_PROPRIO;
    }

    @Override
    public boolean atende(Tenant tenant) {
        return tenant.getPixChave() != null && !tenant.getPixChave().isBlank();
    }

    /** Nao ha o que exigir do cliente: sem CPF, sem gateway. */
    @Override
    public void validar(SolicitacaoCobranca solicitacao) {
    }

    @Override
    public CobrancaSinal criar(Tenant tenant, SolicitacaoCobranca solicitacao) {
        String payload = BrCode.gerar(tenant.getPixChave(), tenant.getPixNomeRecebedor(), tenant.getPixCidadeRecebedor(),
                solicitacao.valor(), ReferenciaPix.doAgendamento(solicitacao.agendamentoId()));
        // sem id no gateway (o Pix estatico nao tem cobranca para consultar) e sem imagem: a tela desenha o QR do "copia e cola"
        return new CobrancaSinal(null, payload, null);
    }

    @Override
    public Optional<String> buscarQrCodeBase64(Tenant tenant, String cobrancaExternaId) {
        return Optional.empty();
    }

    /** Nao existe cobranca a cancelar: o Pix estatico so vale se o cliente pagar E o dono confirmar. */
    @Override
    public void cancelar(Tenant tenant, String cobrancaExternaId) {
    }

}
