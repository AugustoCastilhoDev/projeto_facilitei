import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Slot } from '../models/slot.model';
import { TokenStorageService } from './token-storage.service';

/** Agenda (listagem) e geracao de slots do tenant logado (ver SlotAdminController no backend). */
@Injectable({ providedIn: 'root' })
export class SlotService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  listarAgenda(inicio: string, fim: string, profissionalId?: number): Observable<Slot[]> {
    const params: Record<string, string> = { inicio, fim };
    if (profissionalId) {
      params['profissionalId'] = String(profissionalId);
    }
    return this.http.get<Slot[]>(`${this.tenantBaseUrl()}/slots`, { params });
  }

  gerarSlots(serviceId: number, profissionalId: number, data: string): Observable<Slot[]> {
    return this.http.post<Slot[]>(`${this.tenantBaseUrl()}/services/${serviceId}/slots/gerar`, null, {
      params: { profissionalId, data },
    });
  }

  private tenantBaseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}`;
  }
}
