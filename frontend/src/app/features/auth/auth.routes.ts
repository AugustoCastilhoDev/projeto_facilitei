import { Routes } from '@angular/router';
import { Login } from './login/login';

export const AUTH_ROUTES: Routes = [
  { path: 'login', component: Login },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
];
