import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { PaymentStatus } from '../../../core/models/booking.model';
import { PortalReserva } from '../../../core/models/cliente.model';
import { PortalBookingService } from '../../../core/services/portal-booking.service';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { LoadingBlock } from '../../../shared/loading-block/loading-block';
import { StatusChip, StatusChipTone } from '../../../shared/status-chip/status-chip';

const ROTULOS_STATUS: Record<PaymentStatus, string> = {
  PENDENTE: 'Aguardando pagamento',
  PAGO: 'Confirmada',
  SEM_SINAL: 'Confirmada (sem sinal)',
  EXPIRADO: 'Expirada',
  CANCELADO: 'Cancelada',
};

function toneDoStatus(status: PaymentStatus): StatusChipTone {
  switch (status) {
    case 'PAGO':
    case 'SEM_SINAL':
      return 'success';
    case 'PENDENTE':
      return 'warning';
    default:
      return 'danger';
  }
}

/** Historico de reservas do cliente autenticado, cruzando todos os negocios onde ele agendou pelo portal. */
@Component({
  selector: 'app-minhas-reservas',
  imports: [MatToolbarModule, MatCardModule, MatButtonModule, MatIconModule, EmptyState, LoadingBlock, StatusChip],
  templateUrl: './minhas-reservas.html',
  styleUrl: './minhas-reservas.scss',
})
export class MinhasReservas implements OnInit {
  private readonly portalBookingService = inject(PortalBookingService);
  private readonly router = inject(Router);

  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly reservas = signal<PortalReserva[]>([]);

  protected readonly toneDoStatus = toneDoStatus;

  ngOnInit(): void {
    this.portalBookingService.listarMinhasReservas().subscribe({
      next: (reservas) => {
        this.reservas.set(reservas);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Nao foi possivel carregar suas reservas agora.');
      },
    });
  }

  protected rotuloStatus(status: PaymentStatus): string {
    return ROTULOS_STATUS[status];
  }

  protected dataHoraFormatada(reserva: PortalReserva): string {
    return new Intl.DateTimeFormat('pt-BR', {
      timeZone: 'America/Sao_Paulo',
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date(reserva.dataHora));
  }

  protected voltarParaDiretorio(): void {
    this.router.navigateByUrl('/portal/diretorio');
  }
}
