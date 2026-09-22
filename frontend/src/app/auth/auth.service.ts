import { Injectable } from '@angular/core';
import { MsalService } from '@azure/msal-angular';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private msalService: MsalService) {}

  login() {
    this.msalService.loginRedirect();
  }

  logout() {
    this.msalService.logoutRedirect();
  }

  estaAutenticado(): boolean {
    return this.msalService.instance.getAllAccounts().length > 0;
  }

  obtenerUsuario(): string | null {
    const cuentas = this.msalService.instance.getAllAccounts();
    return cuentas.length > 0 ? cuentas[0].username : null;
  }

  esAdmin(): boolean {
    const cuentas = this.msalService.instance.getAllAccounts();
    if (cuentas.length === 0) return false;

    const claims = cuentas[0].idTokenClaims as any;
    const roles = claims?.roles || [];
    return roles.includes('ADMIN') || roles.includes('Admin');
  }
}