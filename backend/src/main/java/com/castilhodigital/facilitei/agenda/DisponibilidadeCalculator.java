package com.castilhodigital.facilitei.agenda;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calcula em que horarios de um dia um profissional pode comecar um
 * atendimento de certa duracao. Funcao pura, sem Spring, sem banco e sem
 * relogio (o "agora" chega resolvido nos limites): todas as regras de
 * disponibilidade ficam testaveis com exemplos simples, e quem carrega os
 * dados e o DisponibilidadeService.
 *
 * Um horario e oferecido quando:
 *  - o atendimento INTEIRO cabe dentro de um intervalo do expediente (um
 *    servico de 90min nao pode comecar 11:30 se o expediente fecha 12:00);
 *  - o inicio cai na grade do negocio (multiplos de "passoMin" no relogio de
 *    parede: 09:00, 09:30... - nao 09:10 so porque o expediente comeca 09:10);
 *  - nao se sobrepoe a nenhum periodo ocupado (bloqueio ou agendamento).
 *    Sobreposicao e meio-aberta: terminar as 10:30 e outro comecar as 10:30 e valido;
 *  - respeita os limites de antecedencia minima e horizonte maximo.
 *
 * Limitacao conhecida: o expediente e "hora de parede" dentro de um unico dia,
 * entao nao ha turno que atravesse a meia-noite.
 */
public final class DisponibilidadeCalculator {

    /** Intervalo do expediente num dia, em hora de parede (ex.: 09:00 ate 12:00). */
    public record IntervaloDiario(LocalTime inicio, LocalTime fim) {
    }

    /** Periodo em que o profissional esta indisponivel (bloqueio ou agendamento ja existente). */
    public record PeriodoOcupado(Instant inicio, Instant fim) {
    }

    private static final int MINUTOS_NO_DIA = 24 * 60;

    private DisponibilidadeCalculator() {
    }

    /**
     * @param data            dia (no fuso do negocio) para o qual calcular
     * @param fuso            fuso do negocio
     * @param expediente      intervalos de trabalho do profissional naquele dia da semana
     * @param ocupados        bloqueios e agendamentos que tocam o dia (podem comecar antes dele)
     * @param duracao         duracao total do atendimento (soma dos servicos)
     * @param passoMin        passo da grade em minutos (ex.: 30)
     * @param limiteInferior  primeiro instante em que um atendimento pode COMECAR (agora + antecedencia minima)
     * @param limiteSuperior  instante a partir do qual nao se aceita mais marcacao (fim do horizonte); exclusivo
     * @return instantes de inicio possiveis, em ordem crescente
     */
    public static List<Instant> horariosLivres(LocalDate data, ZoneId fuso, List<IntervaloDiario> expediente,
                                                List<PeriodoOcupado> ocupados, Duration duracao, int passoMin,
                                                Instant limiteInferior, Instant limiteSuperior) {
        if (duracao.isZero() || duracao.isNegative()) {
            throw new IllegalArgumentException("A duracao do atendimento deve ser positiva.");
        }
        if (passoMin <= 0) {
            throw new IllegalArgumentException("O passo da grade deve ser positivo.");
        }

        List<Instant> livres = new ArrayList<>();

        for (IntervaloDiario intervalo : expediente.stream().sorted(Comparator.comparing(IntervaloDiario::inicio)).toList()) {
            ZonedDateTime fimDoIntervalo = data.atTime(intervalo.fim()).atZone(fuso);
            ZonedDateTime candidato = primeiroHorarioDaGrade(data, fuso, intervalo.inicio(), passoMin);

            while (candidato != null && !candidato.plus(duracao).isAfter(fimDoIntervalo)) {
                Instant inicio = candidato.toInstant();
                Instant fim = inicio.plus(duracao);

                boolean dentroDosLimites = !inicio.isBefore(limiteInferior) && inicio.isBefore(limiteSuperior);
                if (dentroDosLimites && !sobrepoeAlgum(inicio, fim, ocupados)) {
                    livres.add(inicio);
                }
                candidato = candidato.plusMinutes(passoMin);
            }
        }

        return livres;
    }

    /**
     * Arredonda o inicio do expediente PARA CIMA ate a proxima marca da grade
     * (09:10 com passo 30 -> 09:30). Nulo se a proxima marca cair no dia seguinte.
     */
    private static ZonedDateTime primeiroHorarioDaGrade(LocalDate data, ZoneId fuso, LocalTime inicioExpediente, int passoMin) {
        int minutos = inicioExpediente.getHour() * 60 + inicioExpediente.getMinute();
        boolean temFracao = inicioExpediente.getSecond() > 0 || inicioExpediente.getNano() > 0;
        int resto = minutos % passoMin;
        if (resto != 0 || temFracao) {
            minutos = minutos - resto + passoMin;
        }
        if (minutos >= MINUTOS_NO_DIA) {
            return null;
        }
        return data.atTime(LocalTime.of(minutos / 60, minutos % 60)).atZone(fuso);
    }

    /** Sobreposicao meio-aberta [inicio, fim): encostar (fim de um = inicio do outro) nao conta. */
    private static boolean sobrepoeAlgum(Instant inicio, Instant fim, List<PeriodoOcupado> ocupados) {
        for (PeriodoOcupado ocupado : ocupados) {
            if (inicio.isBefore(ocupado.fim()) && ocupado.inicio().isBefore(fim)) {
                return true;
            }
        }
        return false;
    }

}
