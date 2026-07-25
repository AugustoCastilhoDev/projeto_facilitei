package com.castilhodigital.facilitei.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, Long> {

    /** O codigo ativo mais recente para o telefone (ainda nao usado) - solicitar um novo nao invalida o anterior, so cria outra linha. */
    Optional<CodigoVerificacao> findFirstByTelefoneAndUsadoEmIsNullOrderByCreatedAtDesc(String telefone);

}
