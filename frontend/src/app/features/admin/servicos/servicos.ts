import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Profissional } from '../../../core/models/profissional.model';
import { ServiceOffering } from '../../../core/models/service-offering.model';
import { ProfissionalService } from '../../../core/services/profissional.service';
import { ServiceOfferingService } from '../../../core/services/service-offering.service';
import { ServiceFormDialog } from './service-form-dialog/service-form-dialog';

/** CRUD de servicos do tenant logado (ver ServiceOfferingAdminController no backend). */
@Component({
  selector: 'app-servicos',
  imports: [DecimalPipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './servicos.html',
  styleUrl: './servicos.scss',
})
export class Servicos {
  private readonly serviceOfferingService = inject(ServiceOfferingService);
  private readonly profissionalService = inject(ProfissionalService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly servicos = signal<ServiceOffering[]>([]);
  protected readonly profissionais = signal<Profissional[]>([]);
  protected readonly carregando = signal(false);
  protected readonly colunas = ['nome', 'duracaoMin', 'preco', 'sinalPercentual', 'profissionais', 'status', 'acoes'];

  constructor() {
    // So profissionais ativos podem ser vinculados a um servico (nao faz sentido vincular um profissional desativado).
    this.profissionalService.listar().subscribe((profissionais) => this.profissionais.set(profissionais.filter((p) => p.ativo)));
    this.carregar();
  }

  protected novoServico(): void {
    const ref = this.dialog.open(ServiceFormDialog, {
      width: '420px',
      data: { servico: null, profissionais: this.profissionais() },
    });
    ref.afterClosed().subscribe((salvo) => {
      if (salvo) {
        this.snackBar.open('Servico salvo com sucesso.', 'Fechar', { duration: 3000 });
        this.carregar();
      }
    });
  }

  protected editarServico(servico: ServiceOffering): void {
    const ref = this.dialog.open(ServiceFormDialog, {
      width: '420px',
      data: { servico, profissionais: this.profissionais() },
    });
    ref.afterClosed().subscribe((salvo) => {
      if (salvo) {
        this.snackBar.open('Servico salvo com sucesso.', 'Fechar', { duration: 3000 });
        this.carregar();
      }
    });
  }

  protected desativarServico(servico: ServiceOffering): void {
    if (!confirm(`Desativar o servico "${servico.nome}"?`)) {
      return;
    }

    this.serviceOfferingService.desativar(servico.id).subscribe({
      next: () => {
        this.snackBar.open('Servico desativado.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => this.snackBar.open('Erro ao desativar servico.', 'Fechar', { duration: 4000 }),
    });
  }

  protected ativarServico(servico: ServiceOffering): void {
    this.serviceOfferingService.ativar(servico.id).subscribe({
      next: () => {
        this.snackBar.open('Servico reativado.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => this.snackBar.open('Erro ao reativar servico.', 'Fechar', { duration: 4000 }),
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.serviceOfferingService.listar().subscribe({
      next: (servicos) => {
        this.servicos.set(servicos);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.snackBar.open('Erro ao carregar servicos.', 'Fechar', { duration: 4000 });
      },
    });
  }
}
