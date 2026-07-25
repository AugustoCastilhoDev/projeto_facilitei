import { Injectable } from '@angular/core';

const TOKEN_KEY = 'facilitei.portal.token';
const CLIENTE_ID_KEY = 'facilitei.portal.clienteId';
const NOME_KEY = 'facilitei.portal.nome';

/**
 * Guarda a sessao do cliente final do portal no localStorage - chaves
 * proprias, deliberadamente separadas de TokenStorageService (admin), para
 * as duas sessoes poderem coexistir no mesmo navegador sem colidir (ex.:
 * dono do negocio testando o proprio portal como cliente).
 */
@Injectable({ providedIn: 'root' })
export class PortalTokenStorageService {
  guardarSessao(token: string, clienteId: number, nome: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(CLIENTE_ID_KEY, String(clienteId));
    localStorage.setItem(NOME_KEY, nome);
  }

  obterToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  obterNome(): string | null {
    return localStorage.getItem(NOME_KEY);
  }

  limparSessao(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(CLIENTE_ID_KEY);
    localStorage.removeItem(NOME_KEY);
  }

  estaAutenticado(): boolean {
    return this.obterToken() !== null;
  }
}
