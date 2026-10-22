import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Producto {
  id?: number;
  sku: string;
  nombre: string;
  stockDisponible?: number;
  precio: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private baseUrl = `${environment.catalogApiUrl}/catalog/products`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Producto[]> { return this.http.get<Producto[]>(this.baseUrl); }
  crear(p: Producto): Observable<Producto> { return this.http.post<Producto>(this.baseUrl, p); }
  actualizar(id: number, p: Producto): Observable<Producto> { return this.http.put<Producto>(`${this.baseUrl}/${id}`, p); }
}
