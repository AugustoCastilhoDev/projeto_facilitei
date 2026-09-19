package com.castilhodigital.facilitei.agenda;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * O segredo do link que o dono recebe no WhatsApp para confirmar (ou recusar) um Pix. Precisa ser um segredo
 * DIFERENTE do codigo publico do agendamento - esse o cliente conhece, e com ele o cliente confirmaria o proprio
 * pagamento. Aleatorio de 256 bits (nao adivinhavel); so o SHA-256 vai para o banco, entao nem quem tiver um
 * dump do banco consegue montar um link valido.
 */
final class TokenConfirmacao {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES = 32;
    /** 32 bytes em Base64 URL sem preenchimento = 43 caracteres. */
    private static final java.util.regex.Pattern TOKEN_VALIDO = java.util.regex.Pattern.compile("[A-Za-z0-9_-]{43}");

    private TokenConfirmacao() {
    }

    /** O token em si (vai so no link do WhatsApp, nunca e gravado) e o hash que fica no banco. */
    record Gerado(String valor, String hash) {
    }

    static Gerado gerar() {
        byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Gerado(valor, hash(valor));
    }

    /** So um token no formato que gerar() produz pode existir; qualquer outra coisa nem chega a consulta ao banco. */
    static boolean formatoValido(String token) {
        return token != null && TOKEN_VALIDO.matcher(token).matches();
    }

    /** SHA-256 em hexadecimal (64 caracteres), a forma como o hash e gravado. */
    static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 e obrigatorio em toda JVM", ex);
        }
    }

}
