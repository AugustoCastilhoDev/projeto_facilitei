import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CategoriaNegocio, DiretorioConfig, ROTULOS_CATEGORIA } from '../../../core/models/directory.model';
import { TenantDiretorioService } from '../../../core/services/tenant-diretorio.service';
import { LoadingBlock } from '../../../shared/loading-block/loading-block';
import { StatusChip } from '../../../shared/status-chip/status-chip';

/** O formulario usa string vazia para campos ausentes (padrao ReactiveForms); a API devolve null. */
function paraFormulario(config: DiretorioConfig) {
  return {
    categoria: config.categoria,
    cidade: config.cidade ?? '',
    descricao: config.descricao ?? '',
    visivelNoDiretorio: config.visivelNoDiretorio,
  };
}

/**
 * Tela para o dono do negocio optar por aparecer no diretorio publico do
 * portal do cliente - complemento ao link direto /agendar/:slug, nunca
 * substituto (ver TenantService.atualizarDiretorio no backend: sem
 * categoria+cidade preenchidos, nao da pra ficar visivel).
 */
@Component({
  selector: 'app-diretorio-admin',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    LoadingBlock,
    StatusChip,
  ],
  templateUrl: './diretorio-admin.html',
  styleUrl: './diretorio-admin.scss',
})
export class DiretorioAdmin {
  private readonly tenantDiretorioService = inject(TenantDiretorioService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly categorias = Object.entries(ROTULOS_CATEGORIA) as [CategoriaNegocio, string][];

  protected readonly form = this.fb.nonNullable.group({
    categoria: [null as CategoriaNegocio | null],
    cidade: [''],
    descricao: [''],
    visivelNoDiretorio: [false],
  });

  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);

  constructor() {
    this.carregar();
  }

  protected salvar(): void {
    this.salvando.set(true);
    const valores = this.form.getRawValue();

    this.tenantDiretorioService.atualizar(valores).subscribe({
      next: (config) => {
        this.salvando.set(false);
        this.form.patchValue(paraFormulario(config));
        this.snackBar.open('Configuracao do diretorio salva.', 'Fechar', { duration: 3000 });
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        this.snackBar.open(erro.error?.detail ?? 'Erro ao salvar. Verifique os campos.', 'Fechar', { duration: 4000 });
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.tenantDiretorioService.consultar().subscribe({
      next: (config) => {
        this.form.patchValue(paraFormulario(config));
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.snackBar.open('Erro ao carregar a configuracao do diretorio.', 'Fechar', { duration: 4000 });
      },
    });
  }
}
