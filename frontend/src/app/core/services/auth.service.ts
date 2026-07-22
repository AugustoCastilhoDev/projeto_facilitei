import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, RegistrarTenantRequest, RegistrarTenantResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  login(email: string, senha: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, senha });
  }

  registrar(request: RegistrarTenantRequest): Observable<RegistrarTenantResponse> {
    return this.http.post<RegistrarTenantResponse>(`${environment.apiUrl}/auth/registrar`, request);
  }
}
