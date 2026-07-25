package com.castilhodigital.facilitei.cliente;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    /** A API do MyZap (e a normalizacao ja usada em MyZapNotificationService) espera so digitos, sem "+" nem formatacao. */
    public String normalizarTelefone(String telefone) {
        return telefone.replaceAll("[^0-9]", "");
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente nao encontrado (id=" + id + ")."));
    }

    /**
     * Login por telefone e tambem cadastro na primeira vez (nao ha tela de
     * "criar conta" separada): se o telefone ja tem um Cliente, devolve-o
     * (nome informado e ignorado); senao cria um novo com o nome informado -
     * por isso so exigimos o nome quando o cliente ainda nao existe (ver
     * VerificarCodigoRequest.nome).
     */
    @Transactional
    public Cliente buscarOuCriarPorTelefone(String telefone, String nome) {
        String telefoneNormalizado = normalizarTelefone(telefone);
        return clienteRepository.findByTelefone(telefoneNormalizado)
                .orElseGet(() -> {
                    if (nome == null || nome.isBlank()) {
                        throw new RegraDeNegocioException("Nome e obrigatorio no primeiro acesso.");
                    }
                    Cliente cliente = new Cliente();
                    cliente.setTelefone(telefoneNormalizado);
                    cliente.setNome(nome);
                    return clienteRepository.save(cliente);
                });
    }

}
