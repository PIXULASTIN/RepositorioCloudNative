import { Routes } from '@angular/router';
// import { MsalGuard } from '@azure/msal-angular';
import { HomeComponent } from './home/home.component';
import { PedidosListComponent } from './pedidos/pedidos-list.component';
import { ProductosListComponent } from './productos/productos-list.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },

  // =====================================================================
  // TEMPORAL: mientras se termina de configurar Entra ID, quitamos el
  // guard para poder ver y probar estas vistas sin necesitar login real.
  // ANTES DE ENTREGAR: descomenta el import de arriba y vuelve a agregar
  // "canActivate: [MsalGuard]" en las dos rutas de abajo.
  { path: 'pedidos', component: PedidosListComponent },
  { path: 'productos', component: ProductosListComponent },
  // =====================================================================
];
