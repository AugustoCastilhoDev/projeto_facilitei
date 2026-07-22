package com.castilhodigital.facilitei.professional;

import java.time.LocalTime;
import java.util.List;

public record ProfissionalResponse(
        Long id,
        String nome,
        LocalTime horarioAbertura,
        LocalTime horarioFechamento,
        boolean ativo,
        List<Long> servicoIds,
        List<String> servicoNomes
) {

    public static ProfissionalResponse from(Profissional profissional) {
        List<Long> servicoIds = profissional.getServicos().stream().map(s -> s.getId()).sorted().toList();
        List<String> servicoNomes = profissional.getServicos().stream().map(s -> s.getNome()).sorted().toList();
        return new ProfissionalResponse(
                profissional.getId(),
                profissional.getNome(),
                profissional.getHorarioAbertura(),
                profissional.getHorarioFechamento(),
                profissional.isAtivo(),
                servicoIds,
                servicoNomes);
    }

}
