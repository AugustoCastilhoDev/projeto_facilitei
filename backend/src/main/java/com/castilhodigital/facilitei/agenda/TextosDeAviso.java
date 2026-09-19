package com.castilhodigital.facilitei.agenda;

import com.castilhodigital.facilitei.pix.ReferenciaPix;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Textos e formatos das mensagens de WhatsApp do dono e os que as mensagens do cliente tambem usam (data, hora,
 * dinheiro). Fica separado dos ouvintes porque o aviso de pagamento tardio nasce fora de um evento
 * (AgendamentoPagamentoService), mas tem que falar do mesmo jeito.
 */
final class TextosDeAviso {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private TextosDeAviso() {
    }

    /** O que aconteceu quando o dinheiro do cliente entrou sem haver um horario para ele. */
    enum SemHorario {
        /** Pagou (ou avisou que pagou) depois do prazo e outra pessoa ja ficou com o horario. */
        HORARIO_OCUPADO_POR_OUTRA_PESSOA,
        /** Pagou um agendamento que ja tinha sido cancelado. */
        AGENDAMENTO_CANCELADO
    }

    static String quando(Instant inicio, ZoneId fuso) {
        return DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", PT_BR).withZone(fuso).format(inicio);
    }

    static String hora(Instant instante, ZoneId fuso) {
        return DateTimeFormatter.ofPattern("HH:mm", PT_BR).withZone(fuso).format(instante);
    }

    /** "R$ 1.234,50" (espaco comum, nao o espaco sem quebra que o NumberFormat usa). */
    static String dinheiro(BigDecimal valor) {
        return "R$ " + String.format(PT_BR, "%,.2f", valor);
    }

    /** "(63) 99999-9999" a partir de 5563999999999; o que nao parecer um numero brasileiro segue como veio. */
    static String telefone(String digitos) {
        if (digitos != null && digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            String nacional = digitos.substring(2);
            int corte = nacional.length() - 4;
            return "(" + nacional.substring(0, 2) + ") " + nacional.substring(2, corte) + "-" + nacional.substring(corte);
        }
        return digitos;
    }

    /** O pedido de conferencia que o dono recebe quando o cliente avisa que pagou. O link leva a tela de confirmar/recusar. */
    static String pedidoDeConfirmacao(AgendamentoEvento evento, String linkDeConfirmacao) {
        // "\n" explicito: %n viraria \r\n no Windows e o WhatsApp mostraria uma linha em branco a mais
        return "Pix a conferir: %s avisou que pagou o sinal de %s (%s, %s).\n"
                        .formatted(evento.clienteNome(), dinheiro(evento.valorSinal()),
                                String.join(" + ", evento.servicos()), quando(evento.inicio(), evento.fuso()))
                + "Confira no seu banco (referência %s) e responda por este link:\n%s"
                        .formatted(ReferenciaPix.doAgendamento(evento.agendamentoId()), linkDeConfirmacao);
    }

    /**
     * Dinheiro que entrou por um horario que nao existe mais: ninguem pode deixar isso passar em silencio, e o dono
     * e quem devolve o valor - por isso o aviso traz o nome e o telefone do cliente.
     */
    static String pagamentoSemHorario(AgendamentoView agendamento, SemHorario situacao, boolean apenasAvisoDoCliente) {
        String quem = "%s, fone %s,".formatted(agendamento.clienteNome(), telefone(agendamento.clienteTelefone()));
        String oQue = (apenasAvisoDoCliente ? "avisou que pagou" : "pagou") + " o sinal de "
                + dinheiro(agendamento.valorSinal()) + " do horário de " + quando(agendamento.inicio(), agendamento.fuso());
        String porQue = switch (situacao) {
            case HORARIO_OCUPADO_POR_OUTRA_PESSOA -> "mas o prazo já tinha acabado e o horário foi ocupado por outra pessoa";
            case AGENDAMENTO_CANCELADO -> "mas esse agendamento já estava cancelado";
        };
        String fecho = apenasAvisoDoCliente
                ? "Se o Pix caiu na sua conta, devolva o valor ao cliente."
                : "Devolva o valor ao cliente.";
        return "Atenção: %s %s, %s. %s".formatted(quem, oQue, porQue, fecho);
    }

}
