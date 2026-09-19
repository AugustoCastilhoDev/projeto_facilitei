package com.castilhodigital.facilitei.common.validation;

/**
 * Normalizacao e validacao de CPF/CNPJ pelos digitos verificadores. Aceita o
 * documento com ou sem mascara ("529.982.247-25" ou "52998224725").
 */
public final class CpfCnpjUtils {

    private static final int[] PESOS_CNPJ_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CpfCnpjUtils() {
    }

    /** Remove tudo que nao for digito; devolve null se a entrada for null. */
    public static String somenteDigitos(String documento) {
        return documento == null ? null : documento.replaceAll("\\D", "");
    }

    public static boolean valido(String documento) {
        String digitos = somenteDigitos(documento);
        if (digitos == null || todosIguais(digitos)) {
            return false;
        }
        return switch (digitos.length()) {
            case 11 -> cpfValido(digitos);
            case 14 -> cnpjValido(digitos);
            default -> false;
        };
    }

    private static boolean cpfValido(String cpf) {
        int primeiro = digitoVerificadorCpf(cpf, 9);
        int segundo = digitoVerificadorCpf(cpf, 10);
        return primeiro == digito(cpf, 9) && segundo == digito(cpf, 10);
    }

    /** Pesos decrescentes de (quantidade + 1) ate 2 sobre os primeiros "quantidade" digitos. */
    private static int digitoVerificadorCpf(String cpf, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += digito(cpf, i) * (quantidade + 1 - i);
        }
        return restoParaDigito(soma);
    }

    private static boolean cnpjValido(String cnpj) {
        int primeiro = digitoVerificadorCnpj(cnpj, PESOS_CNPJ_1);
        int segundo = digitoVerificadorCnpj(cnpj, PESOS_CNPJ_2);
        return primeiro == digito(cnpj, 12) && segundo == digito(cnpj, 13);
    }

    private static int digitoVerificadorCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += digito(cnpj, i) * pesos[i];
        }
        return restoParaDigito(soma);
    }

    private static int restoParaDigito(int soma) {
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int digito(String texto, int indice) {
        return texto.charAt(indice) - '0';
    }

    private static boolean todosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

}
