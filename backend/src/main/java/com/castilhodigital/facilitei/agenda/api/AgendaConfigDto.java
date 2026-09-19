package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.agenda.AgendaConfigService.AgendaConfig;
import jakarta.validation.constraints.NotBlank;

/**
 * Parametros da agenda do negocio (pedido e resposta).
 *
 * @param fusoHorario           identificador IANA, ex.: America/Sao_Paulo, America/Manaus
 * @param intervaloAgendaMin    passo da grade de horarios oferecidos: 5, 10, 15, 20, 30 ou 60
 * @param antecedenciaMinimaMin quanto antes do horario o cliente precisa marcar
 * @param horizonteDias         ate quantos dias a frente se aceita marcacao
 */
public record AgendaConfigDto(

        @NotBlank(message = "Informe o fuso horário.")
        String fusoHorario,

        int intervaloAgendaMin,

        int antecedenciaMinimaMin,

        int horizonteDias

) {

    public static AgendaConfigDto de(AgendaConfig config) {
        return new AgendaConfigDto(config.fusoHorario(), config.intervaloAgendaMin(),
                config.antecedenciaMinimaMin(), config.horizonteDias());
    }

    public AgendaConfig paraConfig() {
        return new AgendaConfig(fusoHorario, intervaloAgendaMin, antecedenciaMinimaMin, horizonteDias);
    }

}
