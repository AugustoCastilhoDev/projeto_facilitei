import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { TokenStorageService } from '../../../core/services/token-storage.service';

/** Shell do painel admin: toolbar + navegacao (Agenda / Servicos) + router-outlet para as sub-rotas. */
@Component({
  selector: 'app-dashboard',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatTabsModule, MatButtonModule, MatIconModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  protected readonly tenantSlug = this.tokenStorage.obterTenantSlug();

  protected sair(): void {
    this.tokenStorage.limparSessao();
    this.router.navigateByUrl('/auth/login');
  }
}
