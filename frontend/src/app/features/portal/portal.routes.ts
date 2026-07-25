import { Routes } from '@angular/router';
import { portalAuthGuard } from '../../core/guards/portal-auth.guard';
import { PortalLogin } from './portal-login/portal-login';

export const PORTAL_ROUTES: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: PortalLogin },
  {
    path: 'diretorio',
    canActivate: [portalAuthGuard],
    loadComponent: () => import('./diretorio/diretorio').then((m) => m.Diretorio),
  },
  {
    path: 'negocio/:slug',
    canActivate: [portalAuthGuard],
    loadComponent: () => import('./negocio-detalhe/negocio-detalhe').then((m) => m.NegocioDetalhe),
  },
  {
    path: 'minhas-reservas',
    canActivate: [portalAuthGuard],
    loadComponent: () => import('./minhas-reservas/minhas-reservas').then((m) => m.MinhasReservas),
  },
];
