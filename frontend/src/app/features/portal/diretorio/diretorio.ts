import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CategoriaNegocio, DiretorioTenant, ROTULOS_CATEGORIA } from '../../../core/models/directory.model';
import { PortalDirectoryService } from '../../../core/services/portal-directory.service';
import { PortalTokenStorageService } from '../../../core/services/portal-token-storage.service';
import { BrandMark } from '../../../shared/brand-mark/brand-mark';
import { EmptyState } from '../../../shared/empty-state/empty-state';
import { LoadingBlock } from '../../../shared/loading-block/loading-block';
import { StatusChip } from '../../../shared/status-chip/status-chip';

/** Diretorio publico de negocios: primeira tela do portal apos o login, complemento ao link direto /agendar/:slug. */
@Component({
  selector: 'app-diretorio',
  imports: [
    ReactiveFormsModule,
    MatToolbarModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    BrandMark,
    EmptyState,
    LoadingBlock,
    StatusChip,
  ],
  templateUrl: './diretorio.html',
  styleUrl: './diretorio.scss',
})
export class Diretorio implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly directoryService = inject(PortalDirectoryService);
  private readonly tokenStorage = inject(PortalTokenStorageService);
  private readonly router = inject(Router);

  protected readonly categorias = Object.entries(ROTULOS_CATEGORIA) as [CategoriaNegocio, string][];

  protected readonly form = this.fb.nonNullable.group({
    categoria: [''],
    cidade: [''],
  });

  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly negocios = signal<DiretorioTenant[]>([]);

  protected readonly nomeCliente = this.tokenStorage.obterNome();

  ngOnInit(): void {
    this.carregar();
  }

  protected rotuloCategoria(categoria: CategoriaNegocio | null): string | null {
    return categoria ? ROTULOS_CATEGORIA[categoria] : null;
  }

  protected buscar(): void {
    this.carregar();
  }

  protected abrirNegocio(slug: string): void {
    this.router.navigate(['/portal/negocio', slug]);
  }

  protected irParaMinhasReservas(): void {
    this.router.navigateByUrl('/portal/minhas-reservas');
  }

  protected sair(): void {
    this.tokenStorage.limparSessao();
    this.router.navigateByUrl('/portal/login');
  }

  private carregar(): void {
    const { categoria, cidade } = this.form.getRawValue();
    this.carregando.set(true);
    this.erro.set(null);

    this.directoryService
      .listar((categoria as CategoriaNegocio) || undefined, cidade || undefined)
      .subscribe({
        next: (negocios) => {
          this.negocios.set(negocios);
          this.carregando.set(false);
        },
        error: () => {
          this.carregando.set(false);
          this.erro.set('Nao foi possivel carregar o diretorio agora. Tente novamente.');
        },
      });
  }
}
