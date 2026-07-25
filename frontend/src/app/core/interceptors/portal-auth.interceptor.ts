import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { PortalTokenStorageService } from '../services/portal-token-storage.service';

/** So anexa o Bearer token do cliente em chamadas ao /api/portal (mesma logica de authInterceptor para /api/admin). */
export const portalAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(PortalTokenStorageService);
  const token = tokenStorage.obterToken();
  const isPortalRequest = req.url.includes('/api/portal');

  if (!token || !isPortalRequest) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
