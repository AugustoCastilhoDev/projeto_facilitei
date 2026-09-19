package com.castilhodigital.facilitei.agenda;

/** Por onde o agendamento entrou - util para relatorios e para tratar cada canal de um jeito (ex.: painel nao cobra sinal). */
public enum AgendamentoOrigem {

    /** Pagina publica do negocio (/agendar/:slug), sem login. */
    PUBLICO,

    /** Portal do cliente logado por WhatsApp. */
    PORTAL,

    /** Criado pelo proprio dono/funcionario no painel (cliente que ligou ou apareceu no balcao). */
    PAINEL

}
