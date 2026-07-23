import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Relatorio } from '../../../core/models/relatorio.model';
import { ReportService } from '../../../core/services/report.service';

function formatarIso(data: Date): string {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

function primeiroDiaDoMesIso(): string {
  const hoje = new Date();
  return formatarIso(new Date(hoje.getFullYear(), hoje.getMonth(), 1));
}

function hojeIso(): string {
  return formatarIso(new Date());
}

/** Relatorio basico do tenant logado: faturamento, taxa de nao comparecimento e clientes recorrentes do periodo. */
@Component({
  selector: 'app-relatorios',
  imports: [DatePipe, DecimalPipe, MatCardModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './relatorios.html',
  styleUrl: './relatorios.scss',
})
export class Relatorios {
  private readonly reportService = inject(ReportService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly inicio = signal(primeiroDiaDoMesIso());
  protected readonly fim = signal(hojeIso());
  protected readonly carregando = signal(false);
  protected readonly relatorio = signal<Relatorio | null>(null);
  protected readonly colunas = ['clienteNome', 'clienteTelefone', 'totalAgendamentos', 'ultimoAgendamento'];

  constructor() {
    this.carregar();
  }

  protected onInicioChange(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    if (valor) {
      this.inicio.set(valor);
      this.carregar();
    }
  }

  protected onFimChange(event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    if (valor) {
      this.fim.set(valor);
      this.carregar();
    }
  }

  private carregar(): void {
    this.carregando.set(true);
    this.reportService.gerar(this.inicio(), this.fim()).subscribe({
      next: (relatorio) => {
        this.relatorio.set(relatorio);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.snackBar.open('Erro ao carregar o relatorio.', 'Fechar', { duration: 4000 });
      },
    });
  }
}
