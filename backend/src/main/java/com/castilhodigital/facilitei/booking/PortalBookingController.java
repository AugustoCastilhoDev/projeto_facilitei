package com.castilhodigital.facilitei.booking;

import com.castilhodigital.facilitei.auth.AuthenticatedClienteResolver;
import com.castilhodigital.facilitei.cliente.Cliente;
import com.castilhodigital.facilitei.cliente.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agendamento pelo portal do cliente autenticado - cross-tenant (o cliente
 * escolhe o negocio pelo diretorio, ver PublicDirectoryController, em vez de
 * ja chegar num link fixo /agendar/:slug). O clienteId nunca vem de path/body,
 * so do JWT (ver AuthenticatedClienteResolver) - elimina o vetor de IDOR pela
 * propria forma da API, sem precisar de um guard equivalente ao
 * TenantSecurityGuard.
 */
@RestController
@RequestMapping("/api/portal/reservas")
@RequiredArgsConstructor
public class PortalBookingController {

    private final BookingCheckoutService bookingCheckoutService;
    private final BookingService bookingService;
    private final ClienteService clienteService;
    private final AuthenticatedClienteResolver authenticatedClienteResolver;

    @PostMapping
    public ResponseEntity<PortalReservaResponse> criar(@Valid @RequestBody CriarReservaPortalRequest request) {
        Cliente cliente = clienteAutenticado();
        CheckoutResult resultado = bookingCheckoutService.iniciarCheckout(
                request.slotId(), cliente, request.clienteNome(), request.clienteCpfCnpj());
        return ResponseEntity.status(HttpStatus.CREATED).body(PortalReservaResponse.from(resultado));
    }

    @GetMapping
    public List<PortalReservaResponse> minhasReservas() {
        Long clienteId = authenticatedClienteResolver.clienteIdAtual();
        return bookingService.listarPorCliente(clienteId).stream().map(PortalReservaResponse::from).toList();
    }

    private Cliente clienteAutenticado() {
        return clienteService.buscarPorId(authenticatedClienteResolver.clienteIdAtual());
    }

}
