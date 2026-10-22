import { Component, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { CatalogService, Producto } from '../catalog/catalog.service';
import { OrdersService, Pedido } from '../orders/orders.service';
import { errorMsg } from '../shared/util';

const ESTADOS = ['CREADO', 'ACEPTADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
    <h2>Dashboard</h2>
    <p>Hola, <strong>{{ auth.getUserName() }}</strong>
      <em class="badge">{{ auth.getRoles().join(', ') || 'sin rol asignado' }}</em></p>

    @if (error) { <p class="error">{{ error }}</p> }

    @if (auth.isAdmin) {
      <h3>Supervisión (Admin)</h3>
      <div class="cards">
        <div class="card"><div class="num">{{ pedidos.length }}</div>pedidos totales</div>
        <div class="card"><div class="num">{{ productos.length }}</div>productos</div>
        <div class="card"><div class="num">{{ stockBajo.length }}</div>con stock bajo (&lt; 10)</div>
        <div class="card"><div class="num">{{ valorInventario | currency:'CLP':'symbol':'1.0-0' }}</div>valor inventario</div>
      </div>
      @if (stockBajo.length) {
        <div class="card"><strong>Stock bajo:</strong>
          @for (p of stockBajo; track p.id) { <div>{{ p.nombre }}: {{ p.stockDisponible }} u.</div> }
        </div>
      }
    } @else if (auth.isOperador) {
      <h3>Operación (Operador)</h3>
      <div class="cards">
        <div class="card"><div class="num">{{ cuenta('CREADO') }}</div>por aceptar</div>
        <div class="card"><div class="num">{{ cuenta('ACEPTADO') + cuenta('EN_PREPARACION') }}</div>en preparación</div>
        <div class="card"><div class="num">{{ cuenta('DESPACHADO') }}</div>despachados</div>
      </div>
    } @else {
      <h3>Mis pedidos (Cliente)</h3>
      <div class="cards">
        <div class="card"><div class="num">{{ pedidos.length }}</div>pedidos realizados</div>
        <div class="card"><div class="num">{{ activos }}</div>en curso</div>
        <div class="card"><div class="num">{{ cuenta('ENTREGADO') }}</div>entregados</div>
      </div>
    }

    <h3>Pedidos por estado</h3>
    <div class="cards">
      @for (e of estados; track e) {
        <div class="card"><div class="num">{{ cuenta(e) }}</div>{{ e }}</div>
      }
    </div>
    <a routerLink="/orders"><button>Ir a pedidos</button></a>
    <a routerLink="/catalog"><button class="sec">Ver catálogo</button></a>
  `,
})
export class DashboardComponent implements OnInit {
  estados = ESTADOS;
  pedidos: Pedido[] = [];
  productos: Producto[] = [];
  error = '';

  constructor(public auth: AuthService, private orders: OrdersService, private catalog: CatalogService) {}

  ngOnInit(): void {
    forkJoin({ p: this.orders.listar(), c: this.catalog.listar() }).subscribe({
      next: ({ p, c }) => { this.pedidos = p; this.productos = c; },
      error: (e) => (this.error = errorMsg(e)),
    });
  }

  cuenta(estado: string): number { return this.pedidos.filter((p) => p.estado === estado).length; }
  get activos(): number { return this.pedidos.filter((p) => !['ENTREGADO', 'CANCELADO'].includes(p.estado ?? '')).length; }
  get stockBajo(): Producto[] { return this.productos.filter((p) => (p.stockDisponible ?? 0) < 10); }
  get valorInventario(): number { return this.productos.reduce((s, p) => s + p.precio * (p.stockDisponible ?? 0), 0); }
}
