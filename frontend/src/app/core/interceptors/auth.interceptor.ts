import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';

/**
 * So anexa o Bearer token em chamadas ao /api/admin - as rotas publicas e
 * de auth nao precisam dele, e nao faz sentido manda-lo mesmo que exista um
 * token de uma sessao admin ativa no mesmo navegador (ex.: aba da agenda
 * publica aberta ao lado do painel admin).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const token = tokenStorage.obterToken();
  const isAdminRequest = req.url.includes('/api/admin');

  if (!token || !isAdminRequest) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
