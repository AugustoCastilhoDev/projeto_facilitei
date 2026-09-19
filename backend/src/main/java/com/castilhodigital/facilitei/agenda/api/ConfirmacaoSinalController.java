package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendamentoService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tela do link que o dono recebe no WhatsApp quando um cliente avisa que pagou o sinal (Pix na chave dele): ele
 * confere o extrato e toca em "recebi" ou "nao recebi", sem precisar entrar no painel. Sem login - o segredo do link
 * (256 bits, so o hash fica no banco) e a autorizacao. Um token invalido, de outro agendamento ou substituido por um
 * mais novo responde 404, sem dizer qual dos casos foi.
 *
 * Confirmar e recusar sao idempotentes: o dono que toca duas vezes (ou reabre o link) so ve o estado atual.
 */
@RestController
@RequestMapping("/api/public/confirmacoes-sinal/{token}")
@RequiredArgsConstructor
public class ConfirmacaoSinalController {

    private final AgendamentoService agendamentoService;
    private final Clock clock;

    @GetMapping
    public ConfirmacaoSinalResponse consultar(@PathVariable String token) {
        return ConfirmacaoSinalResponse.de(agendamentoService.buscarPorTokenDoDono(token));
    }

    /** "Recebi": confirma o horario. Se a reserva ja tinha expirado, ela so volta se o horario ainda estiver livre (409 se nao). */
    @PostMapping("/confirmar")
    public ConfirmacaoSinalResponse confirmar(@PathVariable String token) {
        return ConfirmacaoSinalResponse.de(agendamentoService.confirmarPorToken(token, Instant.now(clock)));
    }

    /** "Nao recebi": cancela a reserva, devolve o horario para a agenda e avisa o cliente. */
    @PostMapping("/recusar")
    public ConfirmacaoSinalResponse recusar(@PathVariable String token) {
        return ConfirmacaoSinalResponse.de(agendamentoService.recusarPorToken(token));
    }

}
