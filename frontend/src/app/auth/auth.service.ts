import { Injectable } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private msalService: MsalService) {}

  login(): void {
    this.msalService.loginRedirect();
  }

  logout(): void {
    this.msalService.logoutRedirect();
  }

  isLoggedIn(): boolean {
    return this.msalService.instance.getAllAccounts().length > 0;
  }

  getActiveAccount(): AccountInfo | null {
    return (
      this.msalService.instance.getActiveAccount() ??
      this.msalService.instance.getAllAccounts()[0] ??
      null
    );
  }

  getUsername(): string {
    return this.getActiveAccount()?.username ?? '';
  }
}
