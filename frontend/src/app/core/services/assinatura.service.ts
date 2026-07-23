import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Assinatura } from '../models/assinatura.model';
import { TokenStorageService } from './token-storage.service';

/** Status da assinatura do tenant logado com a PROPRIA plataforma (ver AssinaturaAdminController no backend). */
@Injectable({ providedIn: 'root' })
export class AssinaturaService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  consultar(): Observable<Assinatura> {
    return this.http.get<Assinatura>(this.baseUrl());
  }

  cancelar(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl()}/cancelar`, {});
  }

  private baseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}/assinatura`;
  }
}
