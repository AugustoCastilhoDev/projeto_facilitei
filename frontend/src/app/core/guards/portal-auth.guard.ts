import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PortalTokenStorageService } from '../services/portal-token-storage.service';

/** Protege as rotas /portal/**: sem sessao valida de cliente, redireciona para o login do portal. */
export const portalAuthGuard: CanActivateFn = () => {
  const tokenStorage = inject(PortalTokenStorageService);
  const router = inject(Router);

  if (tokenStorage.estaAutenticado()) {
    return true;
  }

  return router.createUrlTree(['/portal/login']);
};
