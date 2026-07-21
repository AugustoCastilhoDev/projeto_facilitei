import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: parametros de rota (ex.: :slug) chegam como
    // input() do componente automaticamente, sem precisar ler ActivatedRoute na mao.
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    // @angular/animations e provideAnimationsAsync estao deprecated desde o
    // Angular 20.2 (substituidos por animate.enter/animate.leave), com
    // remocao prevista para a v23 - mas ainda sao o caminho suportado hoje
    // (Angular 22) para habilitar ripple/transicoes do Angular Material.
    // TODO: migrar quando o Material adotar oficialmente a nova API nativa.
    provideAnimationsAsync(),
  ],
};
