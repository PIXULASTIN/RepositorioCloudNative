import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Pedido, PedidosService } from './pedidos.service';

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="pedidos">
      <a routerLink="/">Volver</a>
      <h2>Pedidos</h2>

      <form (ngSubmit)="crearPedido()">
        <input [(ngModel)]="nuevoCliente" name="cliente" placeholder="Nombre del cliente" required />
        <button type="submit">Crear pedido</button>
      </form>

      <p *ngIf="error" class="error">{{ error }}</p>

      <table *ngIf="pedidos.length > 0">
        <thead>
          <tr><th>ID</th><th>Cliente</th><th>Estado</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let p of pedidos">
            <td>{{ p.id }}</td>
            <td>{{ p.cliente }}</td>
            <td>{{ p.estado }}</td>
          </tr>
        </tbody>
      </table>

      <p *ngIf="pedidos.length === 0 && !error">No hay pedidos todavia.</p>
    </div>
  `,
  styles: [`
    .pedidos { padding: 2rem; font-family: sans-serif; }
    table { border-collapse: collapse; margin-top: 1rem; }
    th, td { border: 1px solid #ccc; padding: 0.5rem 1rem; text-align: left; }
    .error { color: red; }
    form { margin: 1rem 0; }
  `]
})
export class PedidosListComponent implements OnInit {
  pedidos: Pedido[] = [];
  nuevoCliente = '';
  error = '';

  constructor(private pedidosService: PedidosService) {}

  ngOnInit(): void {
    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.pedidosService.listar().subscribe({
      next: (data) => (this.pedidos = data),
      error: (err) => (this.error = 'No se pudo cargar los pedidos: ' + err.message),
    });
  }

  crearPedido(): void {
    if (!this.nuevoCliente.trim()) return;
    this.pedidosService.crear({ cliente: this.nuevoCliente }).subscribe({
      next: () => {
        this.nuevoCliente = '';
        this.cargarPedidos();
      },
      error: (err) => (this.error = 'No se pudo crear el pedido: ' + err.message),
    });
  }
}
