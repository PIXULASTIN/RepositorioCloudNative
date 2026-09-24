import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
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
import { PublicClientApplication, InteractionType, IPublicClientApplication } from '@azure/msal-browser';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { environment } from '../environments/environment';

// 1) Instancia de MSAL (Authorization Code + PKCE, es lo que usa msal-browser por defecto en SPA)
function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msalConfig.clientId,
      authority: environment.msalConfig.authority,
      redirectUri: environment.msalConfig.redirectUri,
    },
    cache: { cacheLocation: 'localStorage' },
  });
}

// 2) Guard: exige login para las rutas protegidas
function MSALGuardConfigFactory() {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: { scopes: environment.apiScopes },
  };
}

// 3) Interceptor: adjunta el Access Token solo a las llamadas hacia la API (Gateway)
function MSALInterceptorConfigFactory() {
  const protectedResourceMap = new Map<string, Array<string>>();
  protectedResourceMap.set(environment.ordersApiUrl + '/*', environment.apiScopes);
  protectedResourceMap.set(environment.catalogApiUrl + '/*', environment.apiScopes);
  return { interactionType: InteractionType.Redirect, protectedResourceMap };
}

// 4) IMPORTANTE: inicializar MSAL y procesar la vuelta del login ANTES de que arranque la app.
//    Sin esto, al volver de Microsoft nadie procesa el "#code=..." y la sesion no queda iniciada.
function initializeMsal(msal: MsalService) {
  return async () => {
    await firstValueFrom(msal.initialize());
    const result = await msal.instance.handleRedirectPromise();
    if (result?.account) {
      msal.instance.setActiveAccount(result.account);
    } else if (!msal.instance.getActiveAccount()) {
      const accounts = msal.instance.getAllAccounts();
      if (accounts.length > 0) {
        msal.instance.setActiveAccount(accounts[0]);
      }
    }
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
    { provide: APP_INITIALIZER, useFactory: initializeMsal, deps: [MsalService], multi: true },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
  ],
};
