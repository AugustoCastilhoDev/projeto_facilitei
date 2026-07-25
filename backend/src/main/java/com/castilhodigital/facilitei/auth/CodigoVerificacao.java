package com.castilhodigital.facilitei.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Codigo de verificacao (OTP) de uso unico para login/cadastro do cliente
 * final por telefone. Sem entidade Cliente associada por FK de proposito: o
 * telefone pode ainda nao ter um Cliente na primeira solicitacao (o login e
 * tambem o cadastro).
 */
@Entity
@Table(name = "codigos_verificacao")
@Getter
@Setter
@NoArgsConstructor
public class CodigoVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String telefone;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
