import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Assinatura } from '../../../core/models/assinatura.model';
import { PLANOS } from '../../../core/models/plano.model';
import { AssinaturaService } from '../../../core/services/assinatura.service';

/** Status da assinatura do tenant logado com a propria plataforma - ver AssinaturaAdminController no backend. */
@Component({
  selector: 'app-assinatura',
  imports: [MatCardModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './assinatura.html',
  styleUrl: './assinatura.scss',
})
export class AssinaturaPage {
  private readonly assinaturaService = inject(AssinaturaService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly carregando = signal(true);
  protected readonly cancelando = signal(false);
  protected readonly assinatura = signal<Assinatura | null>(null);

  protected readonly nomePlano = computed(() => {
    const plano = this.assinatura()?.plano;
    return PLANOS.find((p) => p.valor === plano)?.nome ?? plano ?? '';
  });

  protected readonly diasRestantesTrial = computed(() => {
    const trialAte = this.assinatura()?.trialAte;
    if (!trialAte) {
      return null;
    }
    const diffMs = new Date(trialAte).getTime() - new Date().setHours(0, 0, 0, 0);
    return Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)));
  });

  constructor() {
    this.carregar();
  }

  protected cancelarAssinatura(): void {
    if (!confirm('Cancelar a assinatura? Voce perde acesso a criar novos horarios/profissionais ate reativar.')) {
      return;
    }

    this.cancelando.set(true);
    this.assinaturaService.cancelar().subscribe({
      next: () => {
        this.cancelando.set(false);
        this.snackBar.open('Assinatura cancelada.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => {
        this.cancelando.set(false);
        this.snackBar.open('Erro ao cancelar assinatura.', 'Fechar', { duration: 4000 });
      },
    });
  }

  protected copiar(valor: string | null | undefined): void {
    if (!valor) {
      return;
    }
    navigator.clipboard.writeText(valor);
    this.snackBar.open('Copiado.', 'Fechar', { duration: 2000 });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.assinaturaService.consultar().subscribe({
      next: (assinatura) => {
        this.assinatura.set(assinatura);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.snackBar.open('Erro ao carregar a assinatura.', 'Fechar', { duration: 4000 });
      },
    });
  }
}
