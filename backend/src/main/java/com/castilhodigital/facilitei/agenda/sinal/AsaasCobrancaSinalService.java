package com.castilhodigital.facilitei.agenda.sinal;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.common.validation.CpfCnpjUtils;
import com.castilhodigital.facilitei.payment.PaymentGatewayService;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.tenant.Tenant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sinal cobrado na conta Asaas do proprio negocio (modelo "traga sua propria
 * conta"): a chave de API e do tenant, a plataforma nunca toca no dinheiro.
 * Confirmacao automatica, pelo webhook da Asaas.
 */
@Component
@RequiredArgsConstructor
class AsaasCobrancaSinalService implements CobrancaSinalService {

    private final PaymentGatewayService paymentGatewayService;

    @Override
    public boolean atende(Tenant tenant) {
        return tenant.getAsaasApiKey() != null && !tenant.getAsaasApiKey().isBlank();
    }

    /** A Asaas rejeita a cobranca sem CPF/CNPJ do pagador (confirmado contra o sandbox real). */
    @Override
    public void validar(SolicitacaoCobranca solicitacao) {
        if (!CpfCnpjUtils.valido(solicitacao.clienteCpfCnpj())) {
            throw new RegraDeNegocioException("Informe um CPF ou CNPJ válido para pagar o sinal.");
        }
    }

    @Override
    public CobrancaSinal criar(Tenant tenant, SolicitacaoCobranca solicitacao) {
        PixChargeResult resultado = paymentGatewayService.criarCobrancaPix(tenant.getAsaasApiKey(), new PixChargeRequest(
                solicitacao.clienteNome(), solicitacao.clienteTelefone(),
                CpfCnpjUtils.somenteDigitos(solicitacao.clienteCpfCnpj()),
                solicitacao.valor(), "agendamento-" + solicitacao.agendamentoId()));
        return new CobrancaSinal(resultado.paymentId(), resultado.payload(), resultado.qrCodeImagemBase64());
    }

    @Override
    public Optional<String> buscarQrCodeBase64(Tenant tenant, String cobrancaExternaId) {
        return Optional.ofNullable(
                paymentGatewayService.buscarQrCodePix(tenant.getAsaasApiKey(), cobrancaExternaId).qrCodeImagemBase64());
    }

    @Override
    public void cancelar(Tenant tenant, String cobrancaExternaId) {
        paymentGatewayService.cancelarCobranca(tenant.getAsaasApiKey(), cobrancaExternaId);
    }

}
