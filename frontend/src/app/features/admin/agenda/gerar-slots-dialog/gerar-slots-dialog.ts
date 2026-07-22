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
import { SlotService } from '../../../../core/services/slot.service';

export interface GerarSlotsDialogData {
  servicos: ServiceOffering[];
  profissionais: Profissional[];
  /** Data (yyyy-MM-dd) atualmente exibida na Agenda, usada para pre-preencher o formulario. */
  dataSelecionada: string;
}

/** Dialog para gerar os slots de um dia a partir do horario de funcionamento do PROFISSIONAL escolhido (ver SlotGenerationService). */
@Component({
  selector: 'app-gerar-slots-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './gerar-slots-dialog.html',
  styleUrl: './gerar-slots-dialog.scss',
})
export class GerarSlotsDialog {
  private readonly fb = inject(FormBuilder);
  private readonly slotService = inject(SlotService);
  private readonly dialogRef = inject(MatDialogRef<GerarSlotsDialog>);

  protected readonly data = inject<GerarSlotsDialogData>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    profissionalId: [null as number | null, Validators.required],
    serviceId: [null as number | null, Validators.required],
    data: [this.data.dataSelecionada, Validators.required],
  });

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly servicosDoProfissional = signal<ServiceOffering[]>([]);

  constructor() {
    this.form.controls.profissionalId.valueChanges.subscribe((profissionalId) => {
      const profissional = this.data.profissionais.find((p) => p.id === profissionalId);
      const servicos = profissional
        ? this.data.servicos.filter((s) => profissional.servicoIds.includes(s.id))
        : [];
      this.servicosDoProfissional.set(servicos);
      this.form.controls.serviceId.setValue(null);
    });
  }

  protected gerar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { profissionalId, serviceId, data } = this.form.getRawValue();
    this.carregando.set(true);
    this.erro.set(null);

    this.slotService.gerarSlots(serviceId!, profissionalId!, data).subscribe({
      next: () => this.dialogRef.close(true),
      error: (erro: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(erro.error?.detail ?? 'Erro ao gerar horarios.');
      },
    });
  }

  protected cancelar(): void {
    this.dialogRef.close(false);
  }
}
