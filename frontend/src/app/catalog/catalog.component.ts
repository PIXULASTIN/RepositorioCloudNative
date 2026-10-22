import { Component, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth/auth.service';
import { errorMsg } from '../shared/util';
import { CatalogService, Producto } from './catalog.service';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [FormsModule, CurrencyPipe],
  template: `
    <h2>Catálogo de productos</h2>

    @if (auth.isAdmin) {
      <div class="card">
        <strong>Nuevo producto</strong>
        <div class="row">
          <input [(ngModel)]="nuevo.sku" placeholder="SKU" />
          <input [(ngModel)]="nuevo.nombre" placeholder="Nombre" />
          <input [(ngModel)]="nuevo.precio" type="number" placeholder="Precio" />
          <input [(ngModel)]="nuevo.stockDisponible" type="number" placeholder="Stock" />
          <button (click)="crear()">Crear</button>
        </div>
      </div>
    }

    @if (error) { <p class="error">{{ error }}</p> }

    <table>
      <thead><tr><th>SKU</th><th>Nombre</th><th>Precio</th><th>Stock</th>@if (auth.isAdmin) { <th></th> }</tr></thead>
      <tbody>
        @for (p of productos; track p.id) {
          <tr>
            @if (editando?.id === p.id) {
              <td><input [(ngModel)]="editando!.sku" /></td>
              <td><input [(ngModel)]="editando!.nombre" /></td>
              <td><input [(ngModel)]="editando!.precio" type="number" /></td>
              <td><input [(ngModel)]="editando!.stockDisponible" type="number" /></td>
              <td>
                <button (click)="guardar()">Guardar</button>
                <button class="sec" (click)="editando = null">Cancelar</button>
              </td>
            } @else {
              <td>{{ p.sku }}</td>
              <td>{{ p.nombre }}</td>
              <td>{{ p.precio | currency:'CLP':'symbol':'1.0-0' }}</td>
              <td>{{ p.stockDisponible }}</td>
              @if (auth.isAdmin) { <td><button class="sec" (click)="editar(p)">Editar</button></td> }
            }
          </tr>
        } @empty {
          <tr><td colspan="5" class="muted">No hay productos todavía.</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class CatalogComponent implements OnInit {
  productos: Producto[] = [];
  nuevo: Producto = { sku: '', nombre: '', precio: 0, stockDisponible: 0 };
  editando: Producto | null = null;
  error = '';

  constructor(public auth: AuthService, private catalog: CatalogService) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.catalog.listar().subscribe({
      next: (d) => { this.productos = d; this.error = ''; },
      error: (e) => (this.error = errorMsg(e)),
    });
  }

  crear(): void {
    if (!this.nuevo.sku.trim() || !this.nuevo.nombre.trim()) { this.error = 'SKU y nombre son obligatorios'; return; }
    this.catalog.crear(this.nuevo).subscribe({
      next: () => { this.nuevo = { sku: '', nombre: '', precio: 0, stockDisponible: 0 }; this.cargar(); },
      error: (e) => (this.error = errorMsg(e)),
    });
  }

  editar(p: Producto): void { this.editando = { ...p }; }

  guardar(): void {
    if (!this.editando?.id) return;
    this.catalog.actualizar(this.editando.id, this.editando).subscribe({
      next: () => { this.editando = null; this.cargar(); },
      error: (e) => (this.error = errorMsg(e)),
    });
  }
}
