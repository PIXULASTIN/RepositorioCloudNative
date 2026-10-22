import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="card" style="max-width: 460px; margin: 3rem auto; text-align: center">
      <h2>Pedidos360</h2>
      @if (!auth.isLoggedIn()) {
        <p>Inicia sesión con tu cuenta Microsoft (Entra ID).</p>
        <button (click)="auth.login()">Iniciar sesión con Microsoft</button>
      } @else {
        <p>Sesión iniciada como <strong>{{ auth.getUserName() }}</strong></p>
        <p>Roles: <em class="badge">{{ auth.getRoles().join(', ') || 'sin rol asignado' }}</em></p>
        <div class="row" style="justify-content:center">
          <a routerLink="/dashboard"><button>Ir al dashboard</button></a>
          <button class="sec" (click)="auth.logout()">Cerrar sesión</button>
        </div>
      }
    </div>
  `,
})
export class LoginComponent {
  constructor(public auth: AuthService) {}
}
