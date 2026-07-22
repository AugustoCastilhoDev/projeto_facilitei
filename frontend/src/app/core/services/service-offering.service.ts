import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ServiceOffering, ServiceOfferingRequest } from '../models/service-offering.model';
import { TokenStorageService } from './token-storage.service';

/** CRUD de servicos do tenant logado (ver ServiceOfferingAdminController no backend). */
@Injectable({ providedIn: 'root' })
export class ServiceOfferingService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  listar(): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>(this.baseUrl());
  }

  criar(request: ServiceOfferingRequest): Observable<ServiceOffering> {
    return this.http.post<ServiceOffering>(this.baseUrl(), request);
  }

  atualizar(serviceId: number, request: ServiceOfferingRequest): Observable<ServiceOffering> {
    return this.http.put<ServiceOffering>(`${this.baseUrl()}/${serviceId}`, request);
  }

  desativar(serviceId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl()}/${serviceId}`);
  }

  ativar(serviceId: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl()}/${serviceId}/ativar`, null);
  }

  private baseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}/services`;
  }
}
