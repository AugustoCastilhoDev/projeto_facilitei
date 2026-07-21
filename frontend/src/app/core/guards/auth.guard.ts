import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';

/** Protege as rotas /admin: sem sessao valida, redireciona para o login. */
export const authGuard: CanActivateFn = () => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  if (tokenStorage.estaAutenticado()) {
    return true;
  }

  return router.createUrlTree(['/auth/login']);
};
