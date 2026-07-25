import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategoriaNegocio, DiretorioTenant } from '../models/directory.model';

/** Diretorio publico de negocios (sem autenticacao) - complemento ao link direto /agendar/:slug. */
@Injectable({ providedIn: 'root' })
export class PortalDirectoryService {
  private readonly http = inject(HttpClient);

  listar(categoria?: CategoriaNegocio, cidade?: string, q?: string): Observable<DiretorioTenant[]> {
    const params: Record<string, string> = {};
    if (categoria) {
      params['categoria'] = categoria;
    }
    if (cidade) {
      params['cidade'] = cidade;
    }
    if (q) {
      params['q'] = q;
    }
    return this.http.get<DiretorioTenant[]>(`${environment.apiUrl}/public/directory`, { params });
  }
}
