import { Injectable } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private msal: MsalService) {}

  private get account() {
    return this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0] ?? null;
  }

  isLoggedIn(): boolean {
    return this.account !== null;
  }

  login(): void {
    this.msal.loginRedirect({ scopes: environment.apiScopes });
  }

  logout(): void {
    this.msal.logoutRedirect({ postLogoutRedirectUri: environment.msalConfig.redirectUri });
  }

  getUserName(): string {
    const a = this.account;
    return a ? a.name || a.username : '';
  }

  /** Roles (App Roles de Entra ID) que vienen en el ID token, en mayusculas. */
  getRoles(): string[] {
    const roles = (this.account?.idTokenClaims as any)?.roles;
    return Array.isArray(roles) ? roles.map((r: any) => String(r).toUpperCase()) : [];
  }

  hasRole(...roles: string[]): boolean {
    const mine = this.getRoles();
    return roles.some((r) => mine.includes(r.toUpperCase()));
  }

  get isAdmin(): boolean { return this.hasRole('ADMIN'); }
  get isOperador(): boolean { return this.hasRole('OPERADOR'); }
  get isCliente(): boolean { return this.hasRole('CLIENTE'); }
  /** ADMIN y OPERADOR ven todos los pedidos; CLIENTE solo los suyos. */
  get esStaff(): boolean { return this.isAdmin || this.isOperador; }
}
