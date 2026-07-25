import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClienteLoginResponse } from '../models/cliente.model';

/** Login/cadastro do cliente final do portal, sempre via codigo de verificacao (OTP) por WhatsApp - sem senha. */
@Injectable({ providedIn: 'root' })
export class PortalAuthService {
  private readonly http = inject(HttpClient);

  solicitarCodigo(telefone: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/portal/solicitar-codigo`, { telefone });
  }

  verificarCodigo(telefone: string, codigo: string, nome: string): Observable<ClienteLoginResponse> {
    return this.http.post<ClienteLoginResponse>(`${environment.apiUrl}/auth/portal/verificar-codigo`, {
      telefone,
      codigo,
      nome,
    });
  }
}
