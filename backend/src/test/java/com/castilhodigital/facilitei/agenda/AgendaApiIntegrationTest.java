package com.castilhodigital.facilitei.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castilhodigital.facilitei.auth.JwtService;
import com.castilhodigital.facilitei.payment.PaymentGatewayException;
import com.castilhodigital.facilitei.payment.PixChargeRequest;
import com.castilhodigital.facilitei.payment.PixChargeResult;
import com.castilhodigital.facilitei.tenant.Tenant;
import com.castilhodigital.facilitei.user.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

/**
 * A API da agenda de ponta a ponta: HTTP -> seguranca real (JWT de verdade) -> controllers -> servicos ->
 * Postgres. Cobre o que os testes de servico nao enxergam: rotas, serializacao (fusos!), validacao de entrada,
 * codigos HTTP e o isolamento entre negocios.
 *
 * Cenario (ver CenarioBarbeariaIntegrationTest): "agora" = domingo 2026-09-20 12:00; Ana faz Corte/Barba/Lavagem
 * na segunda (09-12 e 14-18), Bia faz Corte/Lavagem (10-16). Corte e Barba cobram 30% de sinal.
 */
class AgendaApiIntegrationTest extends CenarioBarbeariaIntegrationTest {

    private static final String PUBLICO = "/api/public/tenants/barbearia/agenda";
    private static final String SEGUNDA_10H = "2026-09-21T10:00:00-03:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AgendamentoCheckoutService checkoutService;

    @Autowired
    private AgendamentoService agendamentoService;

    private Tenant concorrente;
    private String tokenDoDono;
    private String tokenDoConcorrente;

    @BeforeEach
    void criarDonos() {
        concorrente = tenant("concorrente");
        tokenDoDono = jwtService.gerarToken(userService.registrarAdmin(negocio, "dono@barbearia.com", "senha12345"));
        tokenDoConcorrente = jwtService.gerarToken(userService.registrarAdmin(concorrente, "dono@concorrente.com", "senha12345"));
    }

    private String admin(String caminho) {
        return "/api/admin/tenants/" + negocio.getId() + caminho;
    }

    private String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }

    private Map<String, Object> pedidoPublico(List<Long> servicoIds, Long profissionalId, String inicio) {
        java.util.HashMap<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("servicoIds", servicoIds);
        corpo.put("profissionalId", profissionalId);
        corpo.put("inicio", inicio);
        corpo.put("clienteNome", "João");
        corpo.put("clienteTelefone", TELEFONE_DIGITADO);
        corpo.put("clienteCpfCnpj", CPF_VALIDO);
        return corpo;
    }

    private ResultActions postPublico(Map<String, Object> corpo) throws Exception {
        return mockMvc.perform(post(PUBLICO + "/agendamentos").contentType(MediaType.APPLICATION_JSON).content(json(corpo)));
    }

    private ResultActions getAdmin(String caminho, String token) throws Exception {
        return mockMvc.perform(get(admin(caminho)).header("Authorization", "Bearer " + token));
    }

    private ResultActions postAdmin(String caminho, String token, Object corpo) throws Exception {
        return mockMvc.perform(post(admin(caminho)).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(corpo)));
    }

    private ResultActions putAdmin(String caminho, String token, Object corpo) throws Exception {
        return mockMvc.perform(put(admin(caminho)).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(corpo)));
    }

    private ResultActions disponibilidade(Long servicoId, Long profissionalId, String data) throws Exception {
        return mockMvc.perform(get(PUBLICO + "/disponibilidade")
                .param("servicoIds", String.valueOf(servicoId))
                .param("profissionalId", String.valueOf(profissionalId))
                .param("data", data));
    }

    private String codigoDe(ResultActions resposta) throws Exception {
        return objectMapper.readTree(resposta.andReturn().getResponse().getContentAsString()).get("codigo").asString();
    }

    // ================= API publica =================

    @Test
    void profissionaisHabilitadosSaoSoQuemFazTodosOsServicosEscolhidos() throws Exception {
        mockMvc.perform(get(PUBLICO + "/profissionais").param("servicoIds", corte.getId() + "," + barba.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana"));

        mockMvc.perform(get(PUBLICO + "/profissionais").param("servicoIds", String.valueOf(corte.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Ana"))
                .andExpect(jsonPath("$[1].nome").value("Bia"));
    }

    @Test
    void disponibilidadeVemComODeslocamentoDoFusoDoNegocio() throws Exception {
        disponibilidade(lavagem.getId(), ana.getId(), "2026-09-21")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14))
                .andExpect(jsonPath("$[0].inicio").value("2026-09-21T09:00:00-03:00"))
                .andExpect(jsonPath("$[0].fim").value("2026-09-21T09:30:00-03:00"))
                .andExpect(jsonPath("$[0].profissionalNome").value("Ana"));
    }

    @Test
    void qualquerProfissionalUneAsAgendas() throws Exception {
        mockMvc.perform(get(PUBLICO + "/disponibilidade")
                        .param("servicoIds", String.valueOf(lavagem.getId())).param("data", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(18));
    }

    @Test
    void reservaSemSinalJaNasceConfirmadaESemPix() throws Exception {
        postPublico(pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.codigo").isNotEmpty())
                .andExpect(jsonPath("$.inicio").value(SEGUNDA_10H))
                .andExpect(jsonPath("$.valorTotal").value(20.0))
                .andExpect(jsonPath("$.pix").isEmpty())
                .andExpect(jsonPath("$.clienteTelefone").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void reservaComSinalDevolveOPixEPodeSerConsultadaPeloCodigo() throws Exception {
        ResultActions criada = postPublico(pedidoPublico(List.of(corte.getId()), ana.getId(), SEGUNDA_10H))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.valorSinal").value(15.0))
                .andExpect(jsonPath("$.expiraEm").value("2026-09-20T12:15:00-03:00"))
                .andExpect(jsonPath("$.pix.copiaECola").value("00020126payload"))
                .andExpect(jsonPath("$.pix.qrCodeBase64").value("base64img"));
        String codigo = codigoDe(criada);

        // F5 na tela de pagamento: o QR nao e guardado, e rebuscado no gateway
        when(paymentGatewayService.buscarQrCodePix("chave-asaas", "pay_1"))
                .thenReturn(new PixChargeResult("pay_1", "00020126payload", "qr-rebuscado"));
        mockMvc.perform(get(PUBLICO + "/agendamentos/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.pix.qrCodeBase64").value("qr-rebuscado"));
    }

    @Test
    void consultaDeAgendamentoConfirmadoNaoTrazMaisPix() throws Exception {
        String codigo = codigoDe(postPublico(pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H)));

        mockMvc.perform(get(PUBLICO + "/agendamentos/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.pix").isEmpty());
    }

    @Test
    void mesmoHorarioParaOSegundoClienteRetorna409() throws Exception {
        postPublico(pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H)).andExpect(status().isCreated());

        postPublico(pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("horário")));
    }

    @Test
    void dadosInvalidosRetornam400ComOErroDeCadaCampo() throws Exception {
        Map<String, Object> ruim = pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H);
        ruim.put("clienteNome", "");
        ruim.put("clienteCpfCnpj", "52998224726");

        postPublico(ruim)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.clienteNome").exists())
                .andExpect(jsonPath("$.erros.clienteCpfCnpj").value("CPF/CNPJ invalido."));
    }

    @Test
    void telefoneInvalidoRetorna400() throws Exception {
        Map<String, Object> ruim = pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H);
        ruim.put("clienteTelefone", "12345");

        postPublico(ruim).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("telefone")));
    }

    @Test
    void negocioInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/public/tenants/nao-existe/agenda/profissionais").param("servicoIds", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void codigoDeOutroNegocioNaoEhEncontrado() throws Exception {
        String codigo = codigoDe(postPublico(pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H)));

        mockMvc.perform(get("/api/public/tenants/concorrente/agenda/agendamentos/" + codigo)).andExpect(status().isNotFound());
        mockMvc.perform(get(PUBLICO + "/agendamentos/00000000-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
        mockMvc.perform(get(PUBLICO + "/agendamentos/1")).andExpect(status().isBadRequest());
    }

    @Test
    void gatewayForaDoArRetorna502EOHorarioContinuaLivre() throws Exception {
        when(paymentGatewayService.criarCobrancaPix(anyString(), any(PixChargeRequest.class)))
                .thenThrow(new PaymentGatewayException("Asaas fora do ar", null));

        postPublico(pedidoPublico(List.of(corte.getId()), ana.getId(), SEGUNDA_10H)).andExpect(status().isBadGateway());

        postPublico(pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H)).andExpect(status().isCreated());
    }

    // ================= seguranca do painel =================

    @Test
    void painelExigeLoginETokenDeOutroNegocioEBarrado() throws Exception {
        mockMvc.perform(get(admin("/agendamentos")).param("de", "2026-09-21")).andExpect(status().isUnauthorized());

        getAdmin("/agendamentos?de=2026-09-21", tokenDoConcorrente).andExpect(status().isForbidden());
        putAdmin("/agenda-config", tokenDoConcorrente, Map.of("fusoHorario", "America/Sao_Paulo", "intervaloAgendaMin", 30,
                "antecedenciaMinimaMin", 60, "horizonteDias", 60)).andExpect(status().isForbidden());
    }

    // ================= agenda e acoes do painel =================

    @Test
    void agendaDoPainelListaOsAgendamentosDoPeriodoComOsDadosDoCliente() throws Exception {
        postPublico(pedidoPublico(List.of(lavagem.getId()), ana.getId(), SEGUNDA_10H)).andExpect(status().isCreated());
        postAdmin("/agendamentos", tokenDoDono, Map.of("servicoIds", List.of(corte.getId(), barba.getId()),
                "profissionalId", ana.getId(), "inicio", "2026-09-21T14:00:00-03:00",
                "clienteNome", "Maria", "clienteTelefone", "63988887777"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("PAINEL"))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.valorSinal").value(0.0))
                .andExpect(jsonPath("$.fim").value("2026-09-21T15:00:00-03:00"));

        getAdmin("/agendamentos?de=2026-09-21&ate=2026-09-21", tokenDoDono)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clienteNome").value("João"))
                .andExpect(jsonPath("$[0].clienteTelefone").value(TELEFONE))
                .andExpect(jsonPath("$[1].servicos.length()").value(2));

        getAdmin("/agendamentos?de=2026-09-22", tokenDoDono).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void painelPodeEncaixarForaDoExpedienteMasNaoSobrepor() throws Exception {
        Map<String, Object> encaixe = Map.of("servicoIds", List.of(lavagem.getId()), "profissionalId", ana.getId(),
                "inicio", "2026-09-21T20:00:00-03:00", "clienteNome", "Maria", "clienteTelefone", "63988887777");

        postAdmin("/agendamentos", tokenDoDono, encaixe).andExpect(status().isCreated());
        postAdmin("/agendamentos", tokenDoDono, encaixe).andExpect(status().isConflict());
    }

    @Test
    void donoConfirmaOSinalCancelaEEmSeguidaNaoPodeCancelarDeNovo() throws Exception {
        AgendamentoView esperando = checkoutService.iniciar(pedido(List.of(corte.getId()), ana.getId(), sp(10, 0)), AGORA).agendamento();

        postAdmin("/agendamentos/" + esperando.id() + "/confirmar-pagamento", tokenDoDono, Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.sinalConfirmado").value(true));
        postAdmin("/agendamentos/" + esperando.id() + "/cancelar", tokenDoDono, Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO_NEGOCIO"));
        postAdmin("/agendamentos/" + esperando.id() + "/cancelar", tokenDoDono, Map.of())
                .andExpect(status().isBadRequest());
    }

    @Test
    void concluirEMarcarFaltaSoDepoisDeOAtendimentoComecar() throws Exception {
        AgendamentoView futuro = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);
        postAdmin("/agendamentos/" + futuro.id() + "/concluir", tokenDoDono, Map.of())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("ainda não começou")));

        // atendimento de ontem, criado pelo painel (que aceita datas passadas)
        Map<String, Object> passado = Map.of("servicoIds", List.of(lavagem.getId()), "profissionalId", ana.getId(),
                "inicio", "2026-09-19T10:00:00-03:00", "clienteNome", "Maria", "clienteTelefone", "63988887777");
        long feito = objectMapper.readTree(postAdmin("/agendamentos", tokenDoDono, passado).andReturn().getResponse()
                .getContentAsString()).get("id").asLong();
        long faltou = objectMapper.readTree(postAdmin("/agendamentos", tokenDoDono, Map.of("servicoIds", List.of(lavagem.getId()),
                "profissionalId", ana.getId(), "inicio", "2026-09-19T11:00:00-03:00", "clienteNome", "Pedro",
                "clienteTelefone", "63977776666")).andReturn().getResponse().getContentAsString()).get("id").asLong();

        postAdmin("/agendamentos/" + feito + "/concluir", tokenDoDono, Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDO"));
        postAdmin("/agendamentos/" + faltou + "/faltou", tokenDoDono, Map.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FALTOU"));
    }

    @Test
    void agendamentoDeOutroNegocioNaoEhEncontradoNoPainel() throws Exception {
        AgendamentoView meu = agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(10, 0)), AGORA);

        mockMvc.perform(post("/api/admin/tenants/" + concorrente.getId() + "/agendamentos/" + meu.id() + "/cancelar")
                        .header("Authorization", "Bearer " + tokenDoConcorrente))
                .andExpect(status().isNotFound());
    }

    @Test
    void janelaDeConsultaInvalidaOuGrandeDemaisEhRecusada() throws Exception {
        getAdmin("/agendamentos?de=2026-09-21&ate=2026-12-31", tokenDoDono).andExpect(status().isBadRequest());
        getAdmin("/agendamentos?de=2026-09-21&ate=2026-09-01", tokenDoDono).andExpect(status().isBadRequest());
    }

    // ================= horarios de trabalho =================

    @Test
    void expedienteSemanalPodeSerLidoESubstituidoEAAgendaPublicaAcompanha() throws Exception {
        getAdmin("/profissionais/" + ana.getId() + "/horarios", tokenDoDono)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].diaSemana").value("MONDAY"))
                .andExpect(jsonPath("$[0].inicio").value("09:00:00"))
                .andExpect(jsonPath("$[1].inicio").value("14:00:00"));

        putAdmin("/profissionais/" + ana.getId() + "/horarios", tokenDoDono,
                List.of(Map.of("diaSemana", "TUESDAY", "inicio", "08:00:00", "fim", "12:00:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        disponibilidade(lavagem.getId(), ana.getId(), "2026-09-21").andExpect(jsonPath("$.length()").value(0));
        disponibilidade(lavagem.getId(), ana.getId(), "2026-09-22")
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].inicio").value("2026-09-22T08:00:00-03:00"));
    }

    @Test
    void expedienteQueSeSobrepoeOuTemFimAntesDoInicioEhRecusadoComMensagemClara() throws Exception {
        putAdmin("/profissionais/" + ana.getId() + "/horarios", tokenDoDono, List.of(
                Map.of("diaSemana", "TUESDAY", "inicio", "09:00:00", "fim", "12:00:00"),
                Map.of("diaSemana", "TUESDAY", "inicio", "11:00:00", "fim", "14:00:00")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("sobrepõem")));

        putAdmin("/profissionais/" + ana.getId() + "/horarios", tokenDoDono,
                List.of(Map.of("diaSemana", "TUESDAY", "inicio", "12:00:00", "fim", "09:00:00")))
                .andExpect(status().isBadRequest());

        putAdmin("/profissionais/" + ana.getId() + "/horarios", tokenDoDono,
                List.of(Map.of("inicio", "09:00:00", "fim", "12:00:00")))
                .andExpect(status().isBadRequest());

        // nenhum dos PUTs invalidos mexeu no expediente
        getAdmin("/profissionais/" + ana.getId() + "/horarios", tokenDoDono).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void semExpedienteOProfissionalNaoTemHorarios() throws Exception {
        putAdmin("/profissionais/" + ana.getId() + "/horarios", tokenDoDono, List.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        disponibilidade(lavagem.getId(), ana.getId(), "2026-09-21").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void naoSeMexeNoExpedienteDeProfissionalDeOutroNegocio() throws Exception {
        mockMvc.perform(put("/api/admin/tenants/" + concorrente.getId() + "/profissionais/" + ana.getId() + "/horarios")
                        .header("Authorization", "Bearer " + tokenDoConcorrente)
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isNotFound());
    }

    // ================= bloqueios =================

    @Test
    void bloqueioTiraOPeriodoDaAgendaEAvisaQuantosAgendamentosJaMarcadosCaemNele() throws Exception {
        agendamentoService.criar(pedido(List.of(lavagem.getId()), ana.getId(), sp(15, 0)), AGORA);
        Map<String, Object> dentista = Map.of("inicio", "2026-09-21T14:00:00-03:00", "fim", "2026-09-21T16:00:00-03:00", "motivo", "Dentista");

        String resposta = postAdmin("/profissionais/" + ana.getId() + "/bloqueios", tokenDoDono, dentista)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agendamentosAfetados").value(1))
                .andExpect(jsonPath("$.bloqueio.motivo").value("Dentista"))
                .andExpect(jsonPath("$.bloqueio.inicio").value("2026-09-21T14:00:00-03:00"))
                .andReturn().getResponse().getContentAsString();

        disponibilidade(lavagem.getId(), ana.getId(), "2026-09-21")
                // 14 horarios - 1 (15:00 ja ocupado) = 13; o bloqueio 14:00-16:00 tira mais 3 (14:00, 14:30 e 15:30)
                .andExpect(jsonPath("$.length()").value(10));

        long bloqueioId = objectMapper.readTree(resposta).get("bloqueio").get("id").asLong();
        getAdmin("/profissionais/" + ana.getId() + "/bloqueios", tokenDoDono)
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete(admin("/profissionais/" + ana.getId() + "/bloqueios/" + bloqueioId))
                        .header("Authorization", "Bearer " + tokenDoDono))
                .andExpect(status().isNoContent());
        getAdmin("/profissionais/" + ana.getId() + "/bloqueios", tokenDoDono).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void bloqueioComFimAntesDoInicioEhRecusado() throws Exception {
        postAdmin("/profissionais/" + ana.getId() + "/bloqueios", tokenDoDono,
                Map.of("inicio", "2026-09-21T16:00:00-03:00", "fim", "2026-09-21T14:00:00-03:00"))
                .andExpect(status().isBadRequest());
    }

    // ================= configuracao da agenda =================

    @Test
    void configuracaoDaAgendaTemPadroesDeBarbeariaEMudaAGradeDoCliente() throws Exception {
        getAdmin("/agenda-config", tokenDoDono)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fusoHorario").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.intervaloAgendaMin").value(30))
                .andExpect(jsonPath("$.antecedenciaMinimaMin").value(60))
                .andExpect(jsonPath("$.horizonteDias").value(60));

        putAdmin("/agenda-config", tokenDoDono, Map.of("fusoHorario", "America/Sao_Paulo", "intervaloAgendaMin", 60,
                "antecedenciaMinimaMin", 30, "horizonteDias", 30))
                .andExpect(status().isOk()).andExpect(jsonPath("$.intervaloAgendaMin").value(60));

        // grade de 1 em 1 hora: manha 09, 10, 11 e tarde 14, 15, 16, 17
        disponibilidade(lavagem.getId(), ana.getId(), "2026-09-21").andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void configuracaoInvalidaEhRecusada() throws Exception {
        putAdmin("/agenda-config", tokenDoDono, Map.of("fusoHorario", "America/Sao_Paulo", "intervaloAgendaMin", 25,
                "antecedenciaMinimaMin", 60, "horizonteDias", 60))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("intervalo")));
        putAdmin("/agenda-config", tokenDoDono, Map.of("fusoHorario", "Marte/Olympus", "intervaloAgendaMin", 30,
                "antecedenciaMinimaMin", 60, "horizonteDias", 60)).andExpect(status().isBadRequest());
        putAdmin("/agenda-config", tokenDoDono, Map.of("fusoHorario", "America/Sao_Paulo", "intervaloAgendaMin", 30,
                "antecedenciaMinimaMin", 60, "horizonteDias", 0)).andExpect(status().isBadRequest());

        assertThat(tenantRepository.findById(negocio.getId()).orElseThrow().getIntervaloAgendaMin()).isEqualTo(30);
    }

}
