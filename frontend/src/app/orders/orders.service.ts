import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ItemPedido {
  productoId: number;
  nombreProducto?: string;
  cantidad: number;
  precioUnitario?: number;
}

export interface Pedido {
  id?: number;
  cliente?: string;
  estado?: string;
  fechaCreacion?: string;
  creadoPor?: string;
  items: ItemPedido[];
}

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private baseUrl = `${environment.ordersApiUrl}/orders`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Pedido[]> { return this.http.get<Pedido[]>(this.baseUrl); }
  obtener(id: number): Observable<Pedido> { return this.http.get<Pedido>(`${this.baseUrl}/${id}`); }
  crear(pedido: Pedido): Observable<Pedido> { return this.http.post<Pedido>(this.baseUrl, pedido); }
  cambiarEstado(id: number, estado: string): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.baseUrl}/${id}/status`, { estado });
  }
}
