import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Pedido {
  id?: number;
  cliente: string;
  estado?: string;
  fechaCreacion?: string;
}

@Injectable({ providedIn: 'root' })
export class PedidosService {
  // Nota: pasa por el API Gateway (puerto 8080), que internamente
  // reenvia esto a pedidos-service. El frontend no sabe (ni le importa)
  // en que puerto/host esta corriendo cada microservicio.
  private baseUrl = `${environment.apiBaseUrl}/pedidos`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.baseUrl);
  }

  crear(pedido: Pedido): Observable<Pedido> {
    return this.http.post<Pedido>(this.baseUrl, pedido);
  }
}
