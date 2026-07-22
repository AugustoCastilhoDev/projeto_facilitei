import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AsaasConfig } from '../models/asaas-config.model';
import { TokenStorageService } from './token-storage.service';

/** Configuracao da propria conta Asaas do tenant logado (modelo BYOPP - ver TenantAsaasConfigController no backend). */
@Injectable({ providedIn: 'root' })
export class AsaasConfigService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  consultar(): Observable<AsaasConfig> {
    return this.http.get<AsaasConfig>(this.baseUrl());
  }

  atualizar(apiKey: string): Observable<AsaasConfig> {
    return this.http.put<AsaasConfig>(this.baseUrl(), { apiKey });
  }

  private baseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}/asaas-config`;
  }
}
