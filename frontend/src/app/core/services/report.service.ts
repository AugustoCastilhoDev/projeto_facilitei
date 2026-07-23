import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Relatorio } from '../models/relatorio.model';
import { TokenStorageService } from './token-storage.service';

/** Relatorio basico do tenant logado (ver ReportAdminController no backend). */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  gerar(inicio: string, fim: string): Observable<Relatorio> {
    return this.http.get<Relatorio>(`${this.tenantBaseUrl()}/relatorios`, { params: { inicio, fim } });
  }

  private tenantBaseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}`;
  }
}
