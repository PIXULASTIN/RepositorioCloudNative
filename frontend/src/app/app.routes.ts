import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { HomeComponent } from './home/home.component';
import { PedidosListComponent } from './pedidos/pedidos-list.component';
import { ProductosListComponent } from './productos/productos-list.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'pedidos', component: PedidosListComponent, canActivate: [MsalGuard] },
  { path: 'productos', component: ProductosListComponent, canActivate: [MsalGuard] },
];
