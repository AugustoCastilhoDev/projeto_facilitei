import { Injectable } from '@angular/core';

const TOKEN_KEY = 'facilitei.token';
const TENANT_ID_KEY = 'facilitei.tenantId';
const TENANT_SLUG_KEY = 'facilitei.tenantSlug';

/** Guarda a sessao do admin logado no localStorage. Sem estado de servidor (JWT stateless, ver backend). */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  guardarSessao(token: string, tenantId: number, tenantSlug: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(TENANT_ID_KEY, String(tenantId));
    localStorage.setItem(TENANT_SLUG_KEY, tenantSlug);
  }

  obterToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  obterTenantId(): number | null {
    const valor = localStorage.getItem(TENANT_ID_KEY);
    return valor ? Number(valor) : null;
  }

  obterTenantSlug(): string | null {
    return localStorage.getItem(TENANT_SLUG_KEY);
  }

  limparSessao(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TENANT_ID_KEY);
    localStorage.removeItem(TENANT_SLUG_KEY);
  }

  estaAutenticado(): boolean {
    return this.obterToken() !== null;
  }
}
