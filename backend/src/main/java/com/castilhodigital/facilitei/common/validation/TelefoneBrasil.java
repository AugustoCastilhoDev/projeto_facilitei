package com.castilhodigital.facilitei.common.validation;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;

/**
 * Normaliza telefones brasileiros para o formato que o WhatsApp/MyZap usa:
 * so digitos, com o codigo do pais (55). "(63) 99999-9999" e "5563999999999"
 * viram a mesma coisa - importante para o telefone identificar a mesma pessoa
 * no link publico, no portal (login por WhatsApp) e no painel.
 */
public final class TelefoneBrasil {

    private static final String CODIGO_DO_PAIS = "55";

    private TelefoneBrasil() {
    }

    /**
     * Aceita DDD + numero (10 ou 11 digitos, com ou sem mascara) ou ja com o
     * codigo do pais (12 ou 13 digitos).
     *
     * @throws RegraDeNegocioException se nao parecer um telefone brasileiro
     */
    public static String normalizar(String telefone) {
        String digitos = CpfCnpjUtils.somenteDigitos(telefone);
        if (digitos == null) {
            throw telefoneInvalido();
        }
        if (digitos.length() == 10 || digitos.length() == 11) {
            digitos = CODIGO_DO_PAIS + digitos;
        }
        boolean tamanhoValido = digitos.length() == 12 || digitos.length() == 13;
        if (!tamanhoValido || !digitos.startsWith(CODIGO_DO_PAIS) || !numeroNacionalValido(digitos.substring(2))) {
            throw telefoneInvalido();
        }
        return digitos;
    }

    /**
     * DDD de dois digitos (nunca comeca com 0) + numero: celular (11 digitos)
     * comeca com 9; fixo (10 digitos) comeca com 2 a 5. Sem isso, um numero de
     * outro pais com 11 digitos ganharia o prefixo 55 e passaria como brasileiro.
     */
    private static boolean numeroNacionalValido(String nacional) {
        if (nacional.charAt(0) == '0') {
            return false;
        }
        char primeiroDigitoDoNumero = nacional.charAt(2);
        return nacional.length() == 11
                ? primeiroDigitoDoNumero == '9'
                : primeiroDigitoDoNumero >= '2' && primeiroDigitoDoNumero <= '5';
    }

    private static RegraDeNegocioException telefoneInvalido() {
        return new RegraDeNegocioException("Informe um telefone válido com DDD, por exemplo (63) 99999-9999.");
    }

}
