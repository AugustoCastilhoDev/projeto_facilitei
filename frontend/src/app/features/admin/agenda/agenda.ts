import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Profissional } from '../../../core/models/profissional.model';
import { ServiceOffering } from '../../../core/models/service-offering.model';
import { Slot } from '../../../core/models/slot.model';
import { ProfissionalService } from '../../../core/services/profissional.service';
import { ServiceOfferingService } from '../../../core/services/service-offering.service';
import { SlotService } from '../../../core/services/slot.service';
import { GerarSlotsDialog } from './gerar-slots-dialog/gerar-slots-dialog';

interface GrupoAgenda {
  data: string;
  slots: Slot[];
}

const SAO_PAULO_TZ = 'America/Sao_Paulo';
const FORMATADOR_DIA = new Intl.DateTimeFormat('en-CA', {
  timeZone: SAO_PAULO_TZ,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});
const FORMATADOR_HORA = new Intl.DateTimeFormat('pt-BR', {
  timeZone: SAO_PAULO_TZ,
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

function hojeIso(): string {
  const hoje = new Date();
  return formatarIso(hoje);
}

function formatarIso(data: Date): string {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

/**
 * O backend nem sempre serializa dataHora com o offset de Sao Paulo: o
 * endpoint de geracao devolve o objeto recem-criado em memoria (com
 * "-03:00"), mas a listagem da agenda passa por uma consulta ao Postgres e
 * volta normalizada para UTC ("Z") - mesmo instante, representacao
 * diferente. Por isso o dia/hora de exibicao sao sempre calculados a partir
 * do fuso America/Sao_Paulo, nunca por slice() na string ISO.
 */
function diaEmSaoPaulo(dataHoraIso: string): string {
  return FORMATADOR_DIA.format(new Date(dataHoraIso));
}

function agruparPorDia(slots: Slot[]): GrupoAgenda[] {
  const porDia = new Map<string, Slot[]>();
  for (const slot of slots) {
    const dia = diaEmSaoPaulo(slot.dataHora);
    const lista = porDia.get(dia) ?? [];
    lista.push(slot);
    porDia.set(dia, lista);
  }
  return [...porDia.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([data, slots]) => ({
      data,
      slots: slots.sort((a, b) => new Date(a.dataHora).getTime() - new Date(b.dataHora).getTime()),
    }));
}

/** Agenda do admin: visao dia/semana de todos os slots (qualquer status) + acao de gerar horarios. */
@Component({
  selector: 'app-agenda',
  imports: [
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './agenda.html',
  styleUrl: './agenda.scss',
})
export class Agenda {
  private readonly slotService = inject(SlotService);
  private readonly serviceOfferingService = inject(ServiceOfferingService);
  private readonly profissionalService = inject(ProfissionalService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly modo = signal<'dia' | 'semana'>('dia');
  protected readonly data = signal(hojeIso());
  protected readonly carregando = signal(false);
  protected readonly grupos = signal<GrupoAgenda[]>([]);
  protected readonly servicos = signal<ServiceOffering[]>([]);
  protected readonly profissionais = signal<Profissional[]>([]);
  protected readonly filtroProfissionalId = signal<number | null>(null);

  constructor() {
    // So servicos ativos podem ter novos horarios gerados (ver SlotGenerationService no backend).
    this.serviceOfferingService.listar().subscribe((servicos) => this.servicos.set(servicos.filter((s) => s.ativo)));
    this.profissionalService.listar().subscribe((profissionais) =>
      this.profissionais.set(profissionais.filter((p) => p.ativo)),
    );
    this.carregar();
  }

  protected alterarModo(modo: string): void {
    this.modo.set(modo === 'semana' ? 'semana' : 'dia');
    this.carregar();
  }

  protected alterarFiltroProfissional(profissionalId: number | null): void {
    this.filtroProfissionalId.set(profissionalId);
    this.carregar();
  }

  protected horaDoSlot(slot: Slot): string {
    return FORMATADOR_HORA.format(new Date(slot.dataHora));
  }

  protected onDataChange(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    if (valor) {
      this.data.set(valor);
      this.carregar();
    }
  }

  protected abrirGerarSlots(): void {
    const ref = this.dialog.open(GerarSlotsDialog, {
      width: '400px',
      data: { servicos: this.servicos(), profissionais: this.profissionais(), dataSelecionada: this.data() },
    });

    ref.afterClosed().subscribe((gerado) => {
      if (gerado) {
        this.snackBar.open('Horarios gerados com sucesso.', 'Fechar', { duration: 3000 });
        this.carregar();
      }
    });
  }

  private carregar(): void {
    const { inicio, fim } = this.calcularIntervalo();
    this.carregando.set(true);

    this.slotService.listarAgenda(inicio, fim, this.filtroProfissionalId() ?? undefined).subscribe({
      next: (slots) => {
        this.grupos.set(agruparPorDia(slots));
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.snackBar.open('Erro ao carregar a agenda.', 'Fechar', { duration: 4000 });
      },
    });
  }

  private calcularIntervalo(): { inicio: string; fim: string } {
    if (this.modo() === 'dia') {
      return { inicio: this.data(), fim: this.data() };
    }
    const inicio = new Date(`${this.data()}T00:00:00`);
    const fim = new Date(inicio);
    fim.setDate(fim.getDate() + 6);
    return { inicio: this.data(), fim: formatarIso(fim) };
  }
}
