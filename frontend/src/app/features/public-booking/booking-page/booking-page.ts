import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Subscription, interval, switchMap } from 'rxjs';
import { Booking, CriarBookingRequest } from '../../../core/models/booking.model';
import { Profissional } from '../../../core/models/profissional.model';
import { ServiceOffering } from '../../../core/models/service-offering.model';
import { Slot } from '../../../core/models/slot.model';
import { PublicTenant } from '../../../core/models/tenant.model';
import { PublicBookingService } from '../../../core/services/public-booking.service';

type Etapa = 'servicos' | 'profissionais' | 'horarios' | 'dados' | 'pagamento';

function hojeIso(): string {
  const hoje = new Date();
  const ano = hoje.getFullYear();
  const mes = String(hoje.getMonth() + 1).padStart(2, '0');
  const dia = String(hoje.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

/** Frequencia do polling de status do pagamento (aguardando confirmacao do webhook do Asaas). */
const INTERVALO_POLLING_MS = 4000;

/**
 * Pagina publica de agendamento (sem autenticacao): o cliente final escolhe
 * servico -> horario -> preenche seus dados -> gera a cobranca Pix e
 * acompanha a confirmacao do pagamento (via polling ate o webhook do Asaas
 * confirmar, ver AsaasWebhookController no backend).
 */
@Component({
  selector: 'app-booking-page',
  imports: [
    ReactiveFormsModule,
    DecimalPipe,
    MatToolbarModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './booking-page.html',
  styleUrl: './booking-page.scss',
})
export class BookingPage implements OnInit, OnDestroy {
  // preenchidos automaticamente a partir dos parametros da rota (ver
  // withComponentInputBinding em app.config.ts). bookingId so existe na rota
  // /agendar/:slug/reserva/:bookingId, usada para retomar a tela de
  // pagamento apos um F5 (ver ngOnInit).
  slug = input.required<string>();
  bookingId = input<string>();

  private readonly publicBookingService = inject(PublicBookingService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly carregandoInicial = signal(true);
  protected readonly erroInicial = signal<string | null>(null);
  protected readonly tenant = signal<PublicTenant | null>(null);
  protected readonly servicos = signal<ServiceOffering[]>([]);

  protected readonly etapa = signal<Etapa>('servicos');
  protected readonly servicoSelecionado = signal<ServiceOffering | null>(null);

  protected readonly carregandoProfissionais = signal(false);
  protected readonly profissionais = signal<Profissional[]>([]);
  protected readonly profissionalSelecionado = signal<Profissional | null>(null);

  protected readonly data = signal(hojeIso());
  protected readonly carregandoSlots = signal(false);
  protected readonly slotsDoServico = signal<Slot[]>([]);
  protected readonly slotSelecionado = signal<Slot | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    clienteNome: ['', Validators.required],
    clienteTelefone: ['', Validators.required],
    clienteCpfCnpj: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly erroEnvio = signal<string | null>(null);

  protected readonly booking = signal<Booking | null>(null);

  private pollingSub?: Subscription;

  ngOnInit(): void {
    const bookingId = this.bookingId();
    if (bookingId) {
      this.retomarReserva(Number(bookingId));
    } else {
      this.carregarInicial();
    }
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
  }

  protected horaDoSlot(slot: Slot): string {
    return new Intl.DateTimeFormat('pt-BR', {
      timeZone: 'America/Sao_Paulo',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date(slot.dataHora));
  }

  protected selecionarServico(servico: ServiceOffering): void {
    this.servicoSelecionado.set(servico);
    this.etapa.set('profissionais');
    this.carregarProfissionais();
  }

  protected selecionarProfissional(profissional: Profissional): void {
    this.profissionalSelecionado.set(profissional);
    this.etapa.set('horarios');
    this.carregarSlots();
  }

  protected onDataChange(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    if (valor) {
      this.data.set(valor);
      this.carregarSlots();
    }
  }

  protected selecionarSlot(slot: Slot): void {
    this.slotSelecionado.set(slot);
    this.etapa.set('dados');
  }

  protected voltarParaServicos(): void {
    this.etapa.set('servicos');
    this.servicoSelecionado.set(null);
    this.profissionalSelecionado.set(null);
    this.slotSelecionado.set(null);
  }

  protected voltarParaProfissionais(): void {
    this.etapa.set('profissionais');
    this.profissionalSelecionado.set(null);
    this.slotSelecionado.set(null);
  }

  protected voltarParaHorarios(): void {
    this.etapa.set('horarios');
    this.slotSelecionado.set(null);
  }

  protected confirmarReserva(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const slot = this.slotSelecionado();
    if (!slot) {
      return;
    }

    this.enviando.set(true);
    this.erroEnvio.set(null);

    const request: CriarBookingRequest = { slotId: slot.id, ...this.form.getRawValue() };

    this.publicBookingService.criarBooking(this.slug(), request).subscribe({
      next: (booking) => {
        // navega para a URL resumivel (sobrevive a F5); o componente
        // recarregado busca o status/QR Code via retomarReserva() -> GET by id.
        this.router.navigate(['/agendar', this.slug(), 'reserva', String(booking.id)], { replaceUrl: true });
      },
      error: (erro: HttpErrorResponse) => {
        this.enviando.set(false);
        this.erroEnvio.set(erro.error?.detail ?? 'Erro ao criar a reserva. Tente novamente.');
      },
    });
  }

  protected copiarPixPayload(): void {
    const payload = this.booking()?.asaasPixPayload;
    if (!payload) {
      return;
    }
    navigator.clipboard.writeText(payload);
    this.snackBar.open('Codigo Pix copiado.', 'Fechar', { duration: 3000 });
  }

  private carregarInicial(): void {
    const slug = this.slug();
    this.carregandoInicial.set(true);
    this.erroInicial.set(null);

    this.publicBookingService.buscarTenant(slug).subscribe({
      next: (tenant) => {
        this.tenant.set(tenant);
        this.carregarServicos();
      },
      error: () => {
        this.carregandoInicial.set(false);
        this.erroInicial.set('Negocio nao encontrado.');
      },
    });
  }

  private carregarServicos(): void {
    this.publicBookingService.listarServicos(this.slug()).subscribe({
      next: (servicos) => {
        this.servicos.set(servicos);
        this.carregandoInicial.set(false);
      },
      error: () => {
        this.carregandoInicial.set(false);
        this.erroInicial.set('Nao foi possivel carregar os servicos deste negocio.');
      },
    });
  }

  /** Retoma a tela de pagamento a partir da URL /agendar/:slug/reserva/:bookingId (ex.: apos um F5). */
  private retomarReserva(bookingId: number): void {
    const slug = this.slug();
    this.carregandoInicial.set(true);
    this.erroInicial.set(null);

    this.publicBookingService.buscarTenant(slug).subscribe({
      next: (tenant) => {
        this.tenant.set(tenant);
        this.carregandoInicial.set(false);
      },
      error: () => {
        this.carregandoInicial.set(false);
        this.erroInicial.set('Negocio nao encontrado.');
      },
    });

    this.publicBookingService.buscarBooking(slug, bookingId).subscribe({
      next: (booking) => {
        this.booking.set(booking);
        this.etapa.set('pagamento');
        if (booking.statusPagamento === 'PENDENTE') {
          this.iniciarPolling(bookingId);
        }
      },
      error: () => {
        this.carregandoInicial.set(false);
        this.erroInicial.set('Reserva nao encontrada.');
      },
    });
  }

  private carregarProfissionais(): void {
    const servico = this.servicoSelecionado();
    if (!servico) {
      return;
    }

    this.carregandoProfissionais.set(true);
    this.publicBookingService.listarProfissionais(this.slug(), servico.id).subscribe({
      next: (profissionais) => {
        this.profissionais.set(profissionais);
        this.carregandoProfissionais.set(false);
      },
      error: () => {
        this.profissionais.set([]);
        this.carregandoProfissionais.set(false);
      },
    });
  }

  private carregarSlots(): void {
    const servico = this.servicoSelecionado();
    const profissional = this.profissionalSelecionado();
    if (!servico || !profissional) {
      return;
    }

    this.carregandoSlots.set(true);
    this.publicBookingService.listarSlotsDisponiveis(this.slug(), this.data()).subscribe({
      next: (slots) => {
        this.slotsDoServico.set(
          slots.filter((slot) => slot.serviceId === servico.id && slot.profissionalId === profissional.id),
        );
        this.carregandoSlots.set(false);
      },
      error: () => {
        this.slotsDoServico.set([]);
        this.carregandoSlots.set(false);
      },
    });
  }

  private iniciarPolling(bookingId: number): void {
    // o backend rebusca o QR Code no Asaas enquanto o pagamento estiver
    // PENDENTE (ver BookingCheckoutService.buscarStatusAtual), entao o GET
    // de status ja vem completo - sem precisar preservar nada no client.
    this.pollingSub = interval(INTERVALO_POLLING_MS)
      .pipe(switchMap(() => this.publicBookingService.buscarBooking(this.slug(), bookingId)))
      .subscribe((booking) => {
        this.booking.set(booking);
        if (booking.statusPagamento !== 'PENDENTE') {
          this.pollingSub?.unsubscribe();
        }
      });
  }
}
