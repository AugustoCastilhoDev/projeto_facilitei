import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Profissional, ProfissionalRequest } from '../models/profissional.model';
import { TokenStorageService } from './token-storage.service';

/** CRUD de profissionais do tenant logado (ver ProfissionalAdminController no backend). */
@Injectable({ providedIn: 'root' })
export class ProfissionalService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  listar(): Observable<Profissional[]> {
    return this.http.get<Profissional[]>(this.baseUrl());
  }

  criar(request: ProfissionalRequest): Observable<Profissional> {
    return this.http.post<Profissional>(this.baseUrl(), request);
  }

  atualizar(profissionalId: number, request: ProfissionalRequest): Observable<Profissional> {
    return this.http.put<Profissional>(`${this.baseUrl()}/${profissionalId}`, request);
  }

  desativar(profissionalId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl()}/${profissionalId}`);
  }

  ativar(profissionalId: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl()}/${profissionalId}/ativar`, null);
  }

  private baseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}/profissionais`;
  }
}
