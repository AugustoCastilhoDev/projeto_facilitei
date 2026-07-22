import { Routes } from '@angular/router';
import { Dashboard } from './dashboard/dashboard';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: Dashboard,
    children: [
      { path: '', redirectTo: 'agenda', pathMatch: 'full' },
      { path: 'agenda', loadComponent: () => import('./agenda/agenda').then((m) => m.Agenda) },
      { path: 'servicos', loadComponent: () => import('./servicos/servicos').then((m) => m.Servicos) },
      {
        path: 'profissionais',
        loadComponent: () => import('./profissionais/profissionais').then((m) => m.Profissionais),
      },
      { path: 'pagamentos', loadComponent: () => import('./pagamentos/pagamentos').then((m) => m.Pagamentos) },
    ],
  },
];
