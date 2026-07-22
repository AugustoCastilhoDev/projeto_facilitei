import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AsaasConfig } from '../../../core/models/asaas-config.model';
import { AsaasConfigService } from '../../../core/services/asaas-config.service';

/**
 * Configuracao da propria conta Asaas do tenant (modelo BYOPP - "traga sua
 * propria conta de pagamento"): a cobranca Pix do sinal sai direto da conta
 * do negocio, nao da plataforma. Ver docs/configurar-pagamentos.md para o
 * tutorial completo (util caso o cliente prefira configurar sozinho).
 */
@Component({
  selector: 'app-pagamentos',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './pagamentos.html',
  styleUrl: './pagamentos.scss',
})
export class Pagamentos {
  private readonly asaasConfigService = inject(AsaasConfigService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly form = this.fb.nonNullable.group({
    apiKey: ['', Validators.required],
  });

  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly config = signal<AsaasConfig | null>(null);

  constructor() {
    this.carregar();
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando.set(true);
    const { apiKey } = this.form.getRawValue();

    this.asaasConfigService.atualizar(apiKey).subscribe({
      next: (config) => {
        this.salvando.set(false);
        this.config.set(config);
        this.form.reset();
        this.snackBar.open('Chave salva com sucesso.', 'Fechar', { duration: 3000 });
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        this.snackBar.open(erro.error?.detail ?? 'Erro ao salvar a chave.', 'Fechar', { duration: 4000 });
      },
    });
  }

  protected copiar(valor: string | null): void {
    if (!valor) {
      return;
    }
    navigator.clipboard.writeText(valor);
    this.snackBar.open('Copiado.', 'Fechar', { duration: 2000 });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.asaasConfigService.consultar().subscribe({
      next: (config) => {
        this.config.set(config);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.snackBar.open('Erro ao carregar a configuracao de pagamentos.', 'Fechar', { duration: 4000 });
      },
    });
  }
}
