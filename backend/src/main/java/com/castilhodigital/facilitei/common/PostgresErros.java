package com.castilhodigital.facilitei.common;

import java.sql.SQLException;

/** Reconhece erros especificos do PostgreSQL pelo SQLState, sem depender do texto da mensagem (que varia com o idioma do servidor). */
public final class PostgresErros {

    /** exclusion_violation: uma constraint EXCLUDE recusou a linha (ex.: agendamentos sobrepostos). */
    private static final String VIOLACAO_DE_EXCLUSAO = "23P01";

    private PostgresErros() {
    }

    public static boolean ehViolacaoDeExclusao(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof SQLException sql && VIOLACAO_DE_EXCLUSAO.equals(sql.getSQLState())) {
                return true;
            }
            if (atual.getCause() == atual) {
                break;
            }
        }
        return false;
    }

}
