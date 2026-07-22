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
import { ProfissionalFormDialog } from './profissional-form-dialog/profissional-form-dialog';

/** CRUD de profissionais do tenant logado (ver ProfissionalAdminController no backend). */
@Component({
  selector: 'app-profissionais',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTableModule],
  templateUrl: './profissionais.html',
  styleUrl: './profissionais.scss',
})
export class Profissionais {
  private readonly profissionalService = inject(ProfissionalService);
  private readonly serviceOfferingService = inject(ServiceOfferingService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly profissionais = signal<Profissional[]>([]);
  protected readonly servicos = signal<ServiceOffering[]>([]);
  protected readonly carregando = signal(false);
  protected readonly colunas = ['nome', 'horario', 'servicos', 'status', 'acoes'];

  constructor() {
    // So servicos ativos podem ser vinculados a um profissional (nao faz sentido vincular um servico desativado).
    this.serviceOfferingService.listar().subscribe((servicos) => this.servicos.set(servicos.filter((s) => s.ativo)));
    this.carregar();
  }

  protected novoProfissional(): void {
    const ref = this.dialog.open(ProfissionalFormDialog, {
      width: '420px',
      data: { profissional: null, servicos: this.servicos() },
    });
    ref.afterClosed().subscribe((salvo) => {
      if (salvo) {
        this.snackBar.open('Profissional salvo com sucesso.', 'Fechar', { duration: 3000 });
        this.carregar();
      }
    });
  }

  protected editarProfissional(profissional: Profissional): void {
    const ref = this.dialog.open(ProfissionalFormDialog, {
      width: '420px',
      data: { profissional, servicos: this.servicos() },
    });
    ref.afterClosed().subscribe((salvo) => {
      if (salvo) {
        this.snackBar.open('Profissional salvo com sucesso.', 'Fechar', { duration: 3000 });
        this.carregar();
      }
    });
  }

  protected desativarProfissional(profissional: Profissional): void {
    if (!confirm(`Desativar o profissional "${profissional.nome}"?`)) {
      return;
    }

    this.profissionalService.desativar(profissional.id).subscribe({
      next: () => {
        this.snackBar.open('Profissional desativado.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => this.snackBar.open('Erro ao desativar profissional.', 'Fechar', { duration: 4000 }),
    });
  }

  protected ativarProfissional(profissional: Profissional): void {
    this.profissionalService.ativar(profissional.id).subscribe({
      next: () => {
        this.snackBar.open('Profissional reativado.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => this.snackBar.open('Erro ao reativar profissional.', 'Fechar', { duration: 4000 }),
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.profissionalService.listar().subscribe({
      next: (profissionais) => {
        this.profissionais.set(profissionais);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.snackBar.open('Erro ao carregar profissionais.', 'Fechar', { duration: 4000 });
      },
    });
  }
}
