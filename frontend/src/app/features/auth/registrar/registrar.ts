import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PLANOS } from '../../../core/models/plano.model';
import { AuthService } from '../../../core/services/auth.service';

function senhasIguaisValidator(grupo: AbstractControl): ValidationErrors | null {
  const senha = grupo.get('senhaAdmin')?.value;
  const confirmacao = grupo.get('confirmarSenha')?.value;
  return senha && confirmacao && senha !== confirmacao ? { senhasDiferentes: true } : null;
}

function horariosValidator(grupo: AbstractControl): ValidationErrors | null {
  const abertura = grupo.get('horarioAbertura')?.value;
  const fechamento = grupo.get('horarioFechamento')?.value;
  return abertura && fechamento && abertura >= fechamento ? { horarioInvalido: true } : null;
}

const DIACRITICOS = /[\u0300-\u036f]/g;

function slugify(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(DIACRITICOS, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

@Component({
  selector: 'app-registrar',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './registrar.html',
  styleUrl: './registrar.scss',
})
export class Registrar {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  private slugEditadoManualmente = false;

  protected readonly planos = PLANOS;

  protected readonly form = this.fb.nonNullable.group(
    {
      nomeNegocio: ['', Validators.required],
      slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9]+(-[a-z0-9]+)*$/)]],
      horarioAbertura: ['09:00', Validators.required],
      horarioFechamento: ['18:00', Validators.required],
      emailAdmin: ['', [Validators.required, Validators.email]],
      senhaAdmin: ['', [Validators.required, Validators.minLength(8)]],
      confirmarSenha: ['', Validators.required],
      cpfCnpj: ['', Validators.required],
      plano: ['BASICO', Validators.required],
    },
    { validators: [senhasIguaisValidator, horariosValidator] },
  );

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    this.form.controls.nomeNegocio.valueChanges.subscribe((nomeNegocio) => {
      if (!this.slugEditadoManualmente) {
        this.form.controls.slug.setValue(slugify(nomeNegocio), { emitEvent: false });
      }
    });

    this.form.controls.slug.valueChanges.subscribe(() => {
      this.slugEditadoManualmente = true;
    });
  }

  protected cadastrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);
    const { nomeNegocio, slug, horarioAbertura, horarioFechamento, emailAdmin, senhaAdmin, cpfCnpj, plano } =
      this.form.getRawValue();

    this.authService
      .registrar({ nomeNegocio, slug, horarioAbertura, horarioFechamento, emailAdmin, senhaAdmin, cpfCnpj, plano })
      .subscribe({
        next: (resposta) => {
          this.snackBar.open('Cadastro realizado! Faca login para continuar.', 'Fechar', { duration: 4000 });
          this.router.navigate(['/auth/login'], { queryParams: { email: resposta.adminEmail } });
        },
        error: (erro: HttpErrorResponse) => this.tratarErro(erro),
      });
  }

  private tratarErro(erro: HttpErrorResponse): void {
    this.carregando.set(false);
    const body = erro.error as { detail?: string; erros?: Record<string, string> } | null;

    if (erro.status === 400 && body?.erros) {
      for (const [campo, mensagem] of Object.entries(body.erros)) {
        this.form.get(campo)?.setErrors({ servidor: mensagem });
      }
      this.erro.set('Corrija os campos indicados.');
      return;
    }

    if (erro.status === 400 && body?.detail) {
      const detalhe = body.detail.toLowerCase();
      if (detalhe.includes('slug')) {
        this.form.controls.slug.setErrors({ servidor: body.detail });
      } else if (detalhe.includes('email')) {
        this.form.controls.emailAdmin.setErrors({ servidor: body.detail });
      } else {
        this.erro.set(body.detail);
      }
      return;
    }

    this.erro.set('Erro ao cadastrar. Tente novamente.');
  }
}
