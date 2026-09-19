package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.agenda.api.LimitesDaAgendaPublica;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

/**
 * O limite por IP nas rotas publicas que mudam estado (reservar e "ja paguei"), com tetos baixos para caber num teste:
 * 3 reservas e 2 avisos de pagamento por IP. (Os demais testes rodam com o teto praticamente desligado, ver
 * PostgresIntegrationTest.) Usa um contexto proprio: os tetos sao parametros do bean.
 */
@Import(LimiteDeRequisicoesPublicasIntegrationTest.LimitesBaixos.class)
class LimiteDeRequisicoesPublicasIntegrationTest extends CenarioPixProprioIntegrationTest {

    private static final String PUBLICO = "/api/public/tenants/barbearia/agenda";

    @TestConfiguration
    static class LimitesBaixos {
        @Bean
        @Primary
        LimitesDaAgendaPublica limitesBaixos(Clock clock) {
            return new LimitesDaAgendaPublica(clock, 3, 2);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Reserva de um corte (com sinal, Pix proprio) as {@code hora}:00, de um telefone proprio, vinda do IP informado. */
    private ResultActions reservar(String ip, int hora, String telefone) throws Exception {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("servicoIds", List.of(corte.getId()));
        corpo.put("profissionalId", ana.getId());
        corpo.put("inicio", "2026-09-21T%02d:00:00-03:00".formatted(hora));
        corpo.put("clienteNome", "João");
        corpo.put("clienteTelefone", telefone);
        return mockMvc.perform(post(PUBLICO + "/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo))
                .with(requisicao -> {
                    requisicao.setRemoteAddr(ip);
                    return requisicao;
                }));
    }

    private ResultActions informarPagamento(String ip, String codigo) throws Exception {
        return mockMvc.perform(post(PUBLICO + "/agendamentos/" + codigo + "/informar-pagamento").with(requisicao -> {
            requisicao.setRemoteAddr(ip);
            return requisicao;
        }));
    }

    private String codigoDe(ResultActions resposta) throws Exception {
        return objectMapper.readTree(resposta.andReturn().getResponse().getContentAsString()).get("codigo").asString();
    }

    @Test
    void aQuartaReservaDoMesmoIpNaJanelaERecusadaSemSegurarHorarioNenhum() throws Exception {
        reservar("203.0.113.10", 9, "63911110001").andExpect(status().isCreated());
        reservar("203.0.113.10", 10, "63911110002").andExpect(status().isCreated());
        reservar("203.0.113.10", 11, "63911110003").andExpect(status().isCreated());

        reservar("203.0.113.10", 9, "63911110004")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(containsString("Muitas reservas")));

        assertThat(agendamentoRepository.count()).as("a recusada nao segurou horario nenhum").isEqualTo(3);
    }

    /** Quem varia o telefone digitado a cada tentativa nao e barrado pelo teto por telefone - so o IP o pega. */
    @Test
    void trocarOTelefoneNaoDriblaOLimiteDoIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            reservar("203.0.113.11", 9 + i, "6391111000" + (i + 1)).andExpect(status().isCreated());
        }

        reservar("203.0.113.11", 12, "63911119999").andExpect(status().isTooManyRequests());
    }

    @Test
    void outroIpNaoPagaPeloAbusoDeUmIp() throws Exception {
        for (int i = 0; i < 4; i++) {
            reservar("203.0.113.12", 9, "6391111000" + i);   // esgota o IP .12 (a 4a tentativa ja e recusada)
        }

        reservar("203.0.113.99", 10, "63922220001").andExpect(status().isCreated());
    }

    @Test
    void oAvisoDePagamentoTemOSeuProprioLimiteEBarraOSpamNoWhatsAppDoDono() throws Exception {
        // 3 reservas de IPs diferentes (nenhum gasta o teto de reservas do IP que vai avisar), cada uma de um cliente
        String primeira = codigoDe(reservar("198.51.100.1", 9, "63911110001"));
        String segunda = codigoDe(reservar("198.51.100.2", 10, "63911110002"));
        String terceira = codigoDe(reservar("198.51.100.3", 11, "63911110003"));

        informarPagamento("203.0.113.20", primeira).andExpect(status().isOk());
        informarPagamento("203.0.113.20", segunda).andExpect(status().isOk());
        informarPagamento("203.0.113.20", terceira)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(containsString("Muitos avisos de pagamento")));

        assertThat(agendamentoRepository.findAll())
                .filteredOn(a -> a.getStatus() == AgendamentoStatus.AGUARDANDO_CONFIRMACAO)
                .as("o terceiro aviso foi barrado antes de tocar no agendamento").hasSize(2);
        informarPagamento("203.0.113.21", terceira).andExpect(status().isOk());   // outro IP ainda pode avisar
    }

    /** So as rotas que mudam estado sao limitadas: o cliente pode consultar e rever a disponibilidade a vontade. */
    @Test
    void consultasNaoGastamOLimite() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get(PUBLICO + "/disponibilidade").param("servicoIds", String.valueOf(corte.getId()))
                    .param("profissionalId", String.valueOf(ana.getId())).param("data", "2026-09-21")
                    .with(requisicao -> {
                        requisicao.setRemoteAddr("203.0.113.30");
                        return requisicao;
                    })).andExpect(status().isOk());
        }

        reservar("203.0.113.30", 9, "63911110001").andExpect(status().isCreated());
    }

}
