import { Injectable } from '@angular/core';
import { MsalService } from '@azure/msal-angular';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private msalService: MsalService) {}

  isLoggedIn(): boolean {
    return this.msalService.instance.getAllAccounts().length > 0;
  }

  login(): void {
    this.msalService.loginRedirect();
  }

  logout(): void {
    this.msalService.logoutRedirect();
  }

  getUserName(): string {
    const accounts = this.msalService.instance.getAllAccounts();
    if (accounts.length > 0) {
      return accounts[0].name || accounts[0].username;
    }
    return '';
  }
}