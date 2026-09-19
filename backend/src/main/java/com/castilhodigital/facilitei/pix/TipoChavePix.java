package com.castilhodigital.facilitei.pix;

/**
 * Os cinco tipos de chave Pix. O tipo e escolhido por quem cadastra (nao adivinhado pelo valor):
 * 11 digitos podem ser um CPF ou um celular sem o +55, e so o dono sabe qual dos dois e a chave dele.
 */
public enum TipoChavePix {
    CPF,
    CNPJ,
    TELEFONE,
    EMAIL,
    /** Chave aleatoria (EVP): um UUID gerado pelo banco. */
    ALEATORIA
}
