package com.castilhodigital.facilitei.pix;

import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import com.castilhodigital.facilitei.common.validation.CpfCnpjUtils;
import com.castilhodigital.facilitei.common.validation.TelefoneBrasil;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Valida a chave Pix informada pelo dono e a devolve no formato que o BR Code espera:
 * CPF/CNPJ so com digitos, telefone como "+55DDDNUMERO", e-mail em minusculas, chave
 * aleatoria (UUID) em minusculas. Uma chave errada gera um QR que o banco do cliente
 * recusa - ou, pior, que paga outra pessoa - entao aqui nao se aceita "quase certo".
 */
public final class ChavePix {

    /** O campo 26 do BR Code tem no maximo 99 caracteres; sobram 77 para a chave depois do identificador do Pix. */
    static final int TAMANHO_MAXIMO = 77;

    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+\\-]+@[a-z0-9\\-]+(\\.[a-z0-9\\-]+)*\\.[a-z]{2,}$");
    private static final Pattern UUID = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private ChavePix() {
    }

    /**
     * @return a chave normalizada, pronta para ir no BR Code
     * @throws RegraDeNegocioException se o valor nao for uma chave valida do tipo escolhido
     */
    public static String normalizar(TipoChavePix tipo, String valor) {
        if (tipo == null || valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("Informe o tipo e o valor da chave Pix.");
        }
        String limpo = valor.trim();
        return switch (tipo) {
            case CPF -> documento(limpo, 11, "CPF");
            case CNPJ -> documento(limpo, 14, "CNPJ");
            case TELEFONE -> telefone(limpo);
            case EMAIL -> email(limpo);
            case ALEATORIA -> aleatoria(limpo);
        };
    }

    private static String documento(String valor, int digitosEsperados, String nome) {
        String digitos = CpfCnpjUtils.somenteDigitos(valor);
        if (digitos.length() != digitosEsperados || !CpfCnpjUtils.valido(digitos)) {
            throw new RegraDeNegocioException("O " + nome + " informado não é válido.");
        }
        return digitos;
    }

    /** O BR Code exige o telefone com "+55"; quem digita costuma usar "(63) 99999-9999". */
    private static String telefone(String valor) {
        return "+" + TelefoneBrasil.normalizar(valor);
    }

    private static String email(String valor) {
        String minusculo = valor.toLowerCase(Locale.ROOT);
        if (minusculo.length() > TAMANHO_MAXIMO || !EMAIL.matcher(minusculo).matches()) {
            throw new RegraDeNegocioException("O e-mail informado não é uma chave Pix válida.");
        }
        return minusculo;
    }

    private static String aleatoria(String valor) {
        String minusculo = valor.toLowerCase(Locale.ROOT);
        if (!UUID.matcher(minusculo).matches()) {
            throw new RegraDeNegocioException(
                    "A chave aleatória tem o formato 123e4567-e89b-12d3-a456-426614174000 (copie-a do app do seu banco).");
        }
        return minusculo;
    }

}
