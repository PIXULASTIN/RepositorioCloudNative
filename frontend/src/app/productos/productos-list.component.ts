import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Producto, ProductosService } from './productos.service';

@Component({
  selector: 'app-productos-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="productos">
      <a routerLink="/">Volver</a>
      <h2>Productos / Stock</h2>

      <form (ngSubmit)="crearProducto()">
        <input [(ngModel)]="nuevo.sku" name="sku" placeholder="SKU" required />
        <input [(ngModel)]="nuevo.nombre" name="nombre" placeholder="Nombre" required />
        <input [(ngModel)]="nuevo.precio" name="precio" type="number" placeholder="Precio" required />
        <input [(ngModel)]="nuevo.stockDisponible" name="stock" type="number" placeholder="Stock" />
        <button type="submit">Crear producto</button>
      </form>

      <p *ngIf="error" class="error">{{ error }}</p>

      <table *ngIf="productos.length > 0">
        <thead>
          <tr><th>SKU</th><th>Nombre</th><th>Stock</th><th>Precio</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let p of productos">
            <td>{{ p.sku }}</td>
            <td>{{ p.nombre }}</td>
            <td>{{ p.stockDisponible }}</td>
            <td>{{ p.precio | currency:'CLP' }}</td>
          </tr>
        </tbody>
      </table>

      <p *ngIf="productos.length === 0 && !error">No hay productos todavia.</p>
    </div>
  `,
  styles: [`
    .productos { padding: 2rem; font-family: sans-serif; }
    table { border-collapse: collapse; margin-top: 1rem; }
    th, td { border: 1px solid #ccc; padding: 0.5rem 1rem; text-align: left; }
    .error { color: red; }
    form { margin: 1rem 0; display: flex; gap: 0.5rem; }
  `]
})
export class ProductosListComponent implements OnInit {
  productos: Producto[] = [];
  nuevo: Producto = { sku: '', nombre: '', precio: 0, stockDisponible: 0 };
  error = '';

  constructor(private productosService: ProductosService) {}

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.productosService.listar().subscribe({
      next: (data) => (this.productos = data),
      error: (err) => (this.error = 'No se pudo cargar los productos: ' + err.message),
    });
  }

  crearProducto(): void {
    if (!this.nuevo.sku.trim() || !this.nuevo.nombre.trim()) return;
    this.productosService.crear(this.nuevo).subscribe({
      next: () => {
        this.nuevo = { sku: '', nombre: '', precio: 0, stockDisponible: 0 };
        this.cargarProductos();
      },
      error: (err) => (this.error = 'No se pudo crear el producto: ' + err.message),
    });
  }
}
