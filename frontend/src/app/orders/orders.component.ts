import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth/auth.service';
import { CatalogService, Producto } from '../catalog/catalog.service';
import { errorMsg } from '../shared/util';
import { ItemPedido, OrdersService, Pedido } from './orders.service';

// Flujo: CREADO -> ACEPTADO -> EN_PREPARACION -> DESPACHADO -> ENTREGADO  (CANCELADO solo desde CREADO)
const SIGUIENTES: Record<string, string[]> = {
  CREADO: ['ACEPTADO', 'CANCELADO'],
  ACEPTADO: ['EN_PREPARACION'],
  EN_PREPARACION: ['DESPACHADO'],
  DESPACHADO: ['ENTREGADO'],
  ENTREGADO: [],
  CANCELADO: [],
};

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe],
  template: `
    <h2>Pedidos</h2>
    <p class="muted">
      @if (auth.esStaff) { Vista de gestión: ves todos los pedidos. } @else { Ves solo tus pedidos. }
    </p>

    <div class="card">
      <strong>Nuevo pedido</strong>
      @if (auth.esStaff) {
        <div class="row"><input [(ngModel)]="cliente" placeholder="Nombre del cliente" style="width:260px" /></div>
      }
      <div class="row">
        <select [(ngModel)]="productoSel">
          <option [ngValue]="null">-- producto --</option>
          @for (p of productos; track p.id) {
            <option [ngValue]="p.id">{{ p.nombre }} (stock {{ p.stockDisponible }})</option>
          }
        </select>
        <input [(ngModel)]="cantidad" type="number" min="1" style="width:80px" />
        <button class="sec" (click)="agregar()">Agregar</button>
      </div>
      @for (it of nuevoItems; track $index) {
        <div>{{ nombreDe(it.productoId) }} × {{ it.cantidad }}
          <button class="sec" (click)="nuevoItems.splice($index, 1)">quitar</button></div>
      }
      <div class="row"><button (click)="crear()" [disabled]="nuevoItems.length === 0">Crear pedido</button></div>
    </div>

    @if (error) { <p class="error">{{ error }}</p> }
    @if (ok) { <p style="color:#067647">{{ ok }}</p> }

    <table>
      <thead><tr><th>ID</th><th>Cliente</th><th>Estado</th><th>Fecha</th><th>Total</th><th>Acciones</th></tr></thead>
      <tbody>
        @for (p of pedidos; track p.id) {
          <tr>
            <td>{{ p.id }}</td>
            <td>{{ p.cliente }}</td>
            <td><span class="estado">{{ p.estado }}</span></td>
            <td>{{ p.fechaCreacion | date:'dd/MM/yyyy HH:mm' }}</td>
            <td>{{ total(p) | currency:'CLP':'symbol':'1.0-0' }}</td>
            <td>
              <button class="sec" (click)="verDetalle(p.id!)">Detalle</button>
              @if (auth.esStaff) {
                @for (s of siguientes(p); track s) {
                  <button [class.danger]="s === 'CANCELADO'" (click)="cambiar(p, s)">{{ s }}</button>
                }
              }
            </td>
          </tr>
        } @empty {
          <tr><td colspan="6" class="muted">No hay pedidos todavía.</td></tr>
        }
      </tbody>
    </table>

    @if (detalle) {
      <div class="card" style="margin-top:1rem">
        <strong>Pedido #{{ detalle.id }}</strong> — {{ detalle.cliente }} — <span class="estado">{{ detalle.estado }}</span>
        <div class="muted">Creado por {{ detalle.creadoPor }}</div>
        <ul>
          @for (it of detalle.items; track $index) {
            <li>{{ it.nombreProducto }} × {{ it.cantidad }} — {{ it.precioUnitario | currency:'CLP':'symbol':'1.0-0' }} c/u</li>
          }
        </ul>
        <button class="sec" (click)="detalle = null">Cerrar</button>
      </div>
    }
  `,
})
export class OrdersComponent implements OnInit {
  pedidos: Pedido[] = [];
  productos: Producto[] = [];
  nuevoItems: ItemPedido[] = [];
  productoSel: number | null = null;
  cantidad = 1;
  cliente = '';
  detalle: Pedido | null = null;
  error = '';
  ok = '';

  constructor(public auth: AuthService, private orders: OrdersService, private catalog: CatalogService) {}

  ngOnInit(): void {
    this.cargar();
    this.catalog.listar().subscribe({ next: (d) => (this.productos = d), error: (e) => (this.error = errorMsg(e)) });
  }

  cargar(): void {
    this.orders.listar().subscribe({
      next: (d) => (this.pedidos = d),
      error: (e) => (this.error = errorMsg(e)),
    });
  }

  nombreDe(id: number): string { return this.productos.find((p) => p.id === id)?.nombre ?? `#${id}`; }

  agregar(): void {
    if (this.productoSel == null || this.cantidad < 1) return;
    this.nuevoItems.push({ productoId: this.productoSel, cantidad: this.cantidad });
    this.productoSel = null;
    this.cantidad = 1;
  }

  crear(): void {
    this.error = ''; this.ok = '';
    this.orders.crear({ cliente: this.cliente, items: this.nuevoItems }).subscribe({
      next: (p) => { this.ok = `Pedido #${p.id} creado`; this.nuevoItems = []; this.cliente = ''; this.cargar(); },
      error: (e) => (this.error = errorMsg(e)),
    });
  }

  siguientes(p: Pedido): string[] { return SIGUIENTES[p.estado ?? ''] ?? []; }

  cambiar(p: Pedido, estado: string): void {
    this.error = ''; this.ok = '';
    this.orders.cambiarEstado(p.id!, estado).subscribe({
      next: () => {
        this.ok = `Pedido #${p.id} → ${estado}`;
        this.cargar();
        // El stock cambia al ACEPTAR: refrescamos el catalogo
        this.catalog.listar().subscribe((d) => (this.productos = d));
      },
      error: (e) => (this.error = errorMsg(e)),
    });
  }

  verDetalle(id: number): void {
    this.orders.obtener(id).subscribe({ next: (d) => (this.detalle = d), error: (e) => (this.error = errorMsg(e)) });
  }

  total(p: Pedido): number {
    return (p.items ?? []).reduce((s, i) => s + (i.precioUnitario ?? 0) * i.cantidad, 0);
  }
}
