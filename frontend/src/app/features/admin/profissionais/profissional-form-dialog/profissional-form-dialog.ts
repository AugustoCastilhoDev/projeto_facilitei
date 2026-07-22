import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Profissional } from '../../../../core/models/profissional.model';
import { ServiceOffering } from '../../../../core/models/service-offering.model';
import { ProfissionalService } from '../../../../core/services/profissional.service';

export interface ProfissionalFormDialogData {
  profissional: Profissional | null;
  servicos: ServiceOffering[];
}

/** Dialog de criacao/edicao de um profissional (ver ProfissionalAdminController no backend). */
@Component({
  selector: 'app-profissional-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './profissional-form-dialog.html',
  styleUrl: './profissional-form-dialog.scss',
})
export class ProfissionalFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly profissionalService = inject(ProfissionalService);
  private readonly dialogRef = inject(MatDialogRef<ProfissionalFormDialog>);

  protected readonly data = inject<ProfissionalFormDialogData>(MAT_DIALOG_DATA);
  protected readonly editando = this.data.profissional !== null;

  protected readonly form = this.fb.nonNullable.group({
    nome: [this.data.profissional?.nome ?? '', Validators.required],
    horarioAbertura: [this.data.profissional?.horarioAbertura ?? '09:00', Validators.required],
    horarioFechamento: [this.data.profissional?.horarioFechamento ?? '18:00', Validators.required],
    servicoIds: [this.data.profissional?.servicoIds ?? ([] as number[])],
  });

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);
    const request = this.form.getRawValue();

    const obs = this.editando
      ? this.profissionalService.atualizar(this.data.profissional!.id, request)
      : this.profissionalService.criar(request);

    obs.subscribe({
      next: () => this.dialogRef.close(true),
      error: (erro: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(erro.error?.detail ?? 'Erro ao salvar profissional.');
      },
    });
  }

  protected cancelar(): void {
    this.dialogRef.close(false);
  }
}
