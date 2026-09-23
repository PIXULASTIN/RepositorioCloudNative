import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import {
  MsalModule,
  MsalService,
  MsalGuard,
  MsalInterceptor,
  MsalBroadcastService,
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
} from '@azure/msal-angular';
import {
  PublicClientApplication,
  InteractionType,
  IPublicClientApplication,
} from '@azure/msal-browser';

import { routes } from './app.routes';
import { environment } from '../environments/environment';

// 1) Instancia de MSAL, con los datos del App Registration del frontend
function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msalConfig.clientId,
      authority: environment.msalConfig.authority,
      redirectUri: environment.msalConfig.redirectUri,
    },
    cache: {
      cacheLocation: 'localStorage',
    },
  });
}

// 2) Config del Guard: exige login para entrar a rutas protegidas
function MSALGuardConfigFactory() {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: environment.apiScopes,
    },
  };
}

// 3) Config del Interceptor: adjunta el token SOLO a las llamadas hacia el Gateway
function MSALInterceptorConfigFactory() {
  const protectedResourceMap = new Map<string, Array<string>>();
  protectedResourceMap.set(environment.pedidosApiUrl + '/*', environment.apiScopes);
  protectedResourceMap.set(environment.catalogoApiUrl + '/*', environment.apiScopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    importProvidersFrom(MsalModule),
    { provide: MSAL_INSTANCE, useFactory: MSALInstanceFactory },
    { provide: MSAL_GUARD_CONFIG, useFactory: MSALGuardConfigFactory },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: MSALInterceptorConfigFactory },
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
  ]
};
