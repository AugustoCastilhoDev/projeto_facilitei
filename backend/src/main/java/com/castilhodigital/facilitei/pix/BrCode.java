package com.castilhodigital.facilitei.pix;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Monta o "Pix copia e cola" (BR Code, padrao EMV do Banco Central) de uma cobranca para uma chave
 * Pix propria: o mesmo texto vira o QR Code. E a versao "estatica com valor" - o banco do cliente
 * mostra o valor e o recebedor, mas o Pix NAO volta para a plataforma avisando que foi pago (isso so
 * existe em cobranca dinamica, que exige PSP). Por isso a confirmacao e manual (o dono confere no banco).
 *
 * Funcao pura, sem estado. Formato: sequencia de campos "ID (2 digitos) + tamanho (2 digitos) + valor",
 * terminada pelo CRC16 do texto inteiro.
 */
public final class BrCode {

    public static final int TAMANHO_MAXIMO_NOME = 25;
    public static final int TAMANHO_MAXIMO_CIDADE = 15;

    private static final Pattern TXID = Pattern.compile("[A-Za-z0-9]{1,25}");
    private static final Pattern NAO_ALFANUMERICO = Pattern.compile("[^A-Z0-9 ]");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("9999999999.99");

    private BrCode() {
    }

    /**
     * @param chave          ja normalizada (ver ChavePix)
     * @param nomeRecebedor  ate 25 caracteres, so letras/numeros/espaco (use sanitizar)
     * @param cidadeRecebedor ate 15 caracteres, idem
     * @param valor          maior que zero
     * @param txid           identificador da cobranca (1 a 25 letras/numeros): aparece no comprovante do banco
     *                       do dono e permite conciliar "este Pix e do agendamento X"
     */
    public static String gerar(String chave, String nomeRecebedor, String cidadeRecebedor, BigDecimal valor, String txid) {
        if (valor == null || valor.signum() <= 0 || valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O valor do Pix precisa ser maior que zero.");
        }
        if (txid == null || !TXID.matcher(txid).matches()) {
            throw new IllegalArgumentException("txid precisa ter de 1 a 25 letras ou numeros.");
        }
        if (nomeRecebedor.length() > TAMANHO_MAXIMO_NOME || cidadeRecebedor.length() > TAMANHO_MAXIMO_CIDADE) {
            throw new IllegalArgumentException("Nome ou cidade do recebedor acima do limite do BR Code.");
        }

        String contaDoRecebedor = campo("00", "br.gov.bcb.pix") + campo("01", chave);
        String semCrc = campo("00", "01")                 // versao do payload
                + campo("01", "12")                       // 12 = uso unico (cada cobranca tem valor e txid proprios)
                + campo("26", contaDoRecebedor)           // conta do recebedor no Pix
                + campo("52", "0000")                     // categoria do comerciante: nao informada
                + campo("53", "986")                      // moeda: real
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", nomeRecebedor)
                + campo("60", cidadeRecebedor)
                + campo("62", campo("05", txid))          // dados adicionais: o txid
                + "6304";                                 // o CRC entra no calculo com o proprio cabecalho
        return semCrc + crc16(semCrc);
    }

    /**
     * Deixa um texto no formato do BR Code para nome/cidade: sem acento, maiusculo, so letras, numeros
     * e espaco, cortado no limite. Devolve vazio se sobrar nada.
     */
    public static String sanitizar(String texto, int tamanhoMaximo) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String limpo = NAO_ALFANUMERICO.matcher(semAcento.toUpperCase(Locale.ROOT)).replaceAll(" ");
        String semExcessoDeEspaco = ESPACOS.matcher(limpo).replaceAll(" ").trim();
        return semExcessoDeEspaco.length() <= tamanhoMaximo
                ? semExcessoDeEspaco
                : semExcessoDeEspaco.substring(0, tamanhoMaximo).trim();
    }

    private static String campo(String id, String valor) {
        if (valor.length() > 99) {
            throw new IllegalArgumentException("Campo " + id + " do BR Code acima de 99 caracteres.");
        }
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** CRC-16/CCITT-FALSE (polinomio 0x1021, inicio 0xFFFF), em 4 digitos hexadecimais maiusculos, como o Banco Central define. */
    static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

}
