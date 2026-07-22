import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Registrar } from './registrar/registrar';

export const AUTH_ROUTES: Routes = [
  { path: 'login', component: Login },
  { path: 'registrar', component: Registrar },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
