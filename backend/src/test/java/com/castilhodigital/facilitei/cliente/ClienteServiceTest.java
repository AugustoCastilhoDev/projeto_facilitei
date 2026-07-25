package com.castilhodigital.facilitei.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.castilhodigital.facilitei.common.exception.EntidadeNaoEncontradaException;
import com.castilhodigital.facilitei.common.exception.RegraDeNegocioException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(clienteRepository);
    }

    @Test
    void normalizarTelefoneRemoveTudoQueNaoEDigito() {
        assertThat(clienteService.normalizarTelefone("+55 (63) 99999-9999")).isEqualTo("5563999999999");
    }

    @Test
    void buscarPorIdInexistenteLancaExcecao() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(1L)).isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void buscarOuCriarPorTelefoneComTelefoneJaCadastradoDevolveClienteExistenteSemCriarNovo() {
        Cliente existente = new Cliente();
        existente.setTelefone("5563999999999");
        existente.setNome("Maria");
        when(clienteRepository.findByTelefone("5563999999999")).thenReturn(Optional.of(existente));

        Cliente resultado = clienteService.buscarOuCriarPorTelefone("+55 63 99999-9999", "Nome Ignorado");

        assertThat(resultado).isSameAs(existente);
        verify(clienteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void buscarOuCriarPorTelefoneComTelefoneNovoCriaClienteComNomeInformado() {
        when(clienteRepository.findByTelefone("5563999999999")).thenReturn(Optional.empty());
        when(clienteRepository.save(org.mockito.ArgumentMatchers.any(Cliente.class)))
                .thenAnswer(invocation -> {
                    Cliente c = invocation.getArgument(0);
                    ReflectionTestUtils.setField(c, "id", 10L);
                    return c;
                });

        Cliente resultado = clienteService.buscarOuCriarPorTelefone("5563999999999", "Maria");

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getTelefone()).isEqualTo("5563999999999");
        assertThat(captor.getValue().getNome()).isEqualTo("Maria");
        assertThat(resultado.getId()).isEqualTo(10L);
    }

    @Test
    void buscarOuCriarPorTelefoneNovoSemNomeLancaRegraDeNegocio() {
        when(clienteRepository.findByTelefone("5563999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarOuCriarPorTelefone("5563999999999", "  "))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(clienteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

}
