package com.castilhodigital.facilitei.payment.asaas;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * cpfCnpj e opcional: omitido do JSON (em vez de enviado como null) quando
 * o cliente nao informa.
 *
 * Usa "mobilePhone" (nao "phone") porque o telefone que coletamos e o
 * celular/WhatsApp do cliente final - "phone" na Asaas e para numero fixo e
 * rejeita numeros de celular com erro "invalid_phone" (confirmado testando
 * contra o sandbox real).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AsaasCustomerRequest(String name, String mobilePhone, String cpfCnpj) {
}
