import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ServiceOffering } from '../../../../core/models/service-offering.model';
import { ServiceOfferingService } from '../../../../core/services/service-offering.service';

export interface ServiceFormDialogData {
  servico: ServiceOffering | null;
}

/** Dialog de criacao/edicao de um servico (ver ServiceOfferingAdminController no backend). */
@Component({
  selector: 'app-service-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './service-form-dialog.html',
  styleUrl: './service-form-dialog.scss',
})
export class ServiceFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly serviceOfferingService = inject(ServiceOfferingService);
  private readonly dialogRef = inject(MatDialogRef<ServiceFormDialog>);

  protected readonly data = inject<ServiceFormDialogData>(MAT_DIALOG_DATA);
  protected readonly editando = this.data.servico !== null;

  protected readonly form = this.fb.nonNullable.group({
    nome: [this.data.servico?.nome ?? '', Validators.required],
    duracaoMin: [this.data.servico?.duracaoMin ?? 30, [Validators.required, Validators.min(1)]],
    preco: [this.data.servico?.preco ?? 0, [Validators.required, Validators.min(0)]],
    sinalPercentual: [
      this.data.servico?.sinalPercentual ?? 50,
      [Validators.required, Validators.min(0), Validators.max(100)],
    ],
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
      ? this.serviceOfferingService.atualizar(this.data.servico!.id, request)
      : this.serviceOfferingService.criar(request);

    obs.subscribe({
      next: () => this.dialogRef.close(true),
      error: (erro: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(erro.error?.detail ?? 'Erro ao salvar servico.');
      },
    });
  }

  protected cancelar(): void {
    this.dialogRef.close(false);
  }
}
