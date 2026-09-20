import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="home">
      <h1>Pedidos360</h1>

      <div *ngIf="!auth.isLoggedIn()">
        <p>Debes iniciar sesion para ver y crear pedidos.</p>
        <button (click)="auth.login()">Iniciar sesion</button>
      </div>

      <div *ngIf="auth.isLoggedIn()">
        <p>Sesion iniciada como: <strong>{{ auth.getUsername() }}</strong></p>
        <button (click)="auth.logout()">Cerrar sesion</button>
        <nav>
          <a routerLink="/pedidos">Ver pedidos</a> |
          <a routerLink="/productos">Ver productos</a>
        </nav>
      </div>
    </div>
  `,
  styles: [`
    .home { padding: 2rem; font-family: sans-serif; }
    button { padding: 0.5rem 1rem; cursor: pointer; }
    nav { margin-top: 1rem; }
  `]
})
export class HomeComponent {
  constructor(public auth: AuthService) {}
}
