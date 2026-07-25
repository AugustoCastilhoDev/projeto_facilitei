import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DiretorioConfig } from '../models/directory.model';
import { TokenStorageService } from './token-storage.service';

/** Configuracao do tenant logado para aparecer no diretorio publico (ver TenantDiretorioAdminController no backend). */
@Injectable({ providedIn: 'root' })
export class TenantDiretorioService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  consultar(): Observable<DiretorioConfig> {
    return this.http.get<DiretorioConfig>(this.baseUrl());
  }

  atualizar(request: DiretorioConfig): Observable<DiretorioConfig> {
    return this.http.put<DiretorioConfig>(this.baseUrl(), request);
  }

  private baseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}/diretorio`;
  }
}
