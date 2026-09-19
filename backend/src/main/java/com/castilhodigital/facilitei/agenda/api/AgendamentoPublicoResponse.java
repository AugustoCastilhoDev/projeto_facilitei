package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendamentoStatus;
import com.castilhodigital.facilitei.agenda.AgendamentoView;
import com.castilhodigital.facilitei.agenda.CheckoutAgendamento;
import com.castilhodigital.facilitei.agenda.sinal.CobrancaSinal;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * O que a pagina publica precisa saber do agendamento. Identificado so pelo codigo aleatorio (nunca o id
 * sequencial) e sem dados pessoais do cliente alem do necessario: quem tem o link ve o estado da propria reserva.
 *
 * @param pix               presente enquanto ha sinal a pagar (AGUARDANDO_PAGAMENTO); nulo nos demais estados
 * @param confirmacaoManual o sinal e um Pix na chave do dono: depois de pagar, o cliente toca em "Ja paguei" e o
 *                          estabelecimento confere e confirma (AGUARDANDO_CONFIRMACAO). false = confirma sozinho
 */
public record AgendamentoPublicoResponse(
        UUID codigo,
        AgendamentoStatus status,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        String profissionalNome,
        List<Servico> servicos,
        BigDecimal valorTotal,
        BigDecimal valorSinal,
        OffsetDateTime expiraEm,
        boolean confirmacaoManual,
        Pix pix
) {

    public record Servico(String nome, int duracaoMin, BigDecimal preco) {
    }

    /** Pix "copia e cola" e, quando disponivel, o QR em PNG base64 para um &lt;img src="data:image/png;base64,..."&gt;. */
    public record Pix(String copiaECola, String qrCodeBase64) {
    }

    public static AgendamentoPublicoResponse de(CheckoutAgendamento checkout) {
        AgendamentoView agendamento = checkout.agendamento();
        ZoneId fuso = agendamento.fuso();
        CobrancaSinal cobranca = checkout.cobranca();
        return new AgendamentoPublicoResponse(
                agendamento.codigoPublico(),
                agendamento.status(),
                agendamento.inicio().atZone(fuso).toOffsetDateTime(),
                agendamento.fim().atZone(fuso).toOffsetDateTime(),
                agendamento.profissionalNome(),
                agendamento.servicos().stream()
                        .map(servico -> new Servico(servico.nome(), servico.duracaoMin(), servico.preco()))
                        .toList(),
                agendamento.valorTotal(),
                agendamento.valorSinal(),
                agendamento.expiraEm() == null ? null : agendamento.expiraEm().atZone(fuso).toOffsetDateTime(),
                agendamento.meioCobranca() != null && agendamento.meioCobranca().confirmacaoManual(),
                cobranca == null ? null : new Pix(cobranca.payload(), cobranca.qrCodeBase64()));
    }

}
