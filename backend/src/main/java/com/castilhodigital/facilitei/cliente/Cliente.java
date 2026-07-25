package com.castilhodigital.facilitei.cliente;

import com.castilhodigital.facilitei.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cliente final que agenda pelo portal (login por telefone/WhatsApp),
 * cruzando varios tenants - diferente de User, que e o dono do negocio
 * preso a exatamente um tenant. Sem senha: a autenticacao e sempre via
 * codigo de verificacao (ver auth.CodigoVerificacaoService).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente extends BaseEntity {

    /** Sempre normalizado (so digitos, ex.: "5563999999999") - ver ClienteService.normalizarTelefone. */
    @Column(nullable = false, unique = true)
    private String telefone;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf_cnpj", length = 20)
    private String cpfCnpj;

}
