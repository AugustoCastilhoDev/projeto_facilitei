import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PortalAuthService } from '../../../core/services/portal-auth.service';
import { PortalTokenStorageService } from '../../../core/services/portal-token-storage.service';
import { BrandMark } from '../../../shared/brand-mark/brand-mark';

type Etapa = 'telefone' | 'codigo';

/**
 * Login/cadastro do cliente final do portal - sempre por telefone + codigo
 * de verificacao via WhatsApp (OTP), sem senha. O nome so e exigido pelo
 * backend na primeira vez (telefone novo); o formulario sempre pede o campo,
 * mas nao valida como obrigatorio aqui - deixa o backend decidir e mostra a
 * mensagem de erro dele quando for o caso.
 */
@Component({
  selector: 'app-portal-login',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    BrandMark,
  ],
  templateUrl: './portal-login.html',
  styleUrl: './portal-login.scss',
})
export class PortalLogin {
  private readonly fb = inject(FormBuilder);
  private readonly portalAuthService = inject(PortalAuthService);
  private readonly tokenStorage = inject(PortalTokenStorageService);
  private readonly router = inject(Router);

  protected readonly etapa = signal<Etapa>('telefone');
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly formTelefone = this.fb.nonNullable.group({
    telefone: ['', Validators.required],
  });

  protected readonly formCodigo = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nome: [''],
  });

  protected solicitarCodigo(): void {
    if (this.formTelefone.invalid) {
      this.formTelefone.markAllAsTouched();
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    this.portalAuthService.solicitarCodigo(this.telefone).subscribe({
      next: () => {
        this.carregando.set(false);
        this.etapa.set('codigo');
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(this.mensagemDeErro(erro, 'Nao foi possivel enviar o codigo agora. Tente novamente.'));
      },
    });
  }

  protected verificarCodigo(): void {
    if (this.formCodigo.invalid) {
      this.formCodigo.markAllAsTouched();
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);
    const { codigo, nome } = this.formCodigo.getRawValue();

    this.portalAuthService.verificarCodigo(this.telefone, codigo, nome).subscribe({
      next: (resposta) => {
        this.tokenStorage.guardarSessao(resposta.token, resposta.clienteId, resposta.nome);
        this.router.navigateByUrl('/portal/diretorio');
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(this.mensagemDeErro(erro, 'Erro ao verificar o codigo. Tente novamente.'));
      },
    });
  }

  protected voltarParaTelefone(): void {
    this.etapa.set('telefone');
    this.erro.set(null);
  }

  protected reenviarCodigo(): void {
    this.solicitarCodigo();
  }

  protected get telefoneAtual(): string {
    return this.telefone;
  }

  private get telefone(): string {
    return this.formTelefone.getRawValue().telefone;
  }

  private mensagemDeErro(erro: HttpErrorResponse, padrao: string): string {
    if (erro.status === 401) {
      return 'Codigo invalido ou expirado.';
    }
    if (erro.status === 429 || erro.status === 400) {
      return erro.error?.detail ?? padrao;
    }
    return padrao;
  }
}
