# Pedidos360

Sistema de gestión de pedidos y catálogo de productos con stock, construido como proyecto de **DSY1107 — Desarrollo Cloud Native I**.

**Integrantes:** _(completar con los nombres del grupo)_

## Arquitectura

```
Usuario
  ↓
Angular + MSAL (Authorization Code + PKCE)
  ↓
Microsoft Entra ID  ──►  Access Token (JWT)
  ↓
AWS API Gateway (HTTP API)  ──  JWT Authorizer + CORS
  ↓
EC2 (Docker Compose)
  ├── pedidos-service   (Spring Boot + Spring Security, :8081)
  └── catalogo-service  (Spring Boot + Spring Security, :8082)
  ↓
Amazon RDS PostgreSQL  (bases: pedidos360 y catalogo360)
```

| Componente | Tecnología |
|---|---|
| Frontend | Angular 18, `@azure/msal-angular` (Guard + Interceptor) |
| Identidad | Microsoft Entra ID (OAuth 2.0 / OIDC, PKCE) |
| Gateway | AWS API Gateway HTTP API con JWT Authorizer y CORS |
| Backend | Java 17, Spring Boot 3.3, Spring Security (Resource Server JWT) |
| Persistencia | PostgreSQL (RDS en la nube, contenedor Postgres en local) |
| Contenedores | Docker y Docker Compose |

## Estructura del repositorio

```
├── backend/
│   ├── pedidos-service/     # Pedidos y estados (puerto 8081)
│   └── catalogo-service/    # Productos, precios y stock (puerto 8082)
├── frontend/                # Angular + MSAL
├── aws/
│   └── crear-api-gateway.sh # Crea el API Gateway (JWT + CORS + rutas)
├── docker-compose.yml       # Ejecución local (incluye Postgres)
├── docker-compose.aws.yml   # Despliegue en EC2 (usa RDS)
└── init-db.sql              # Crea la base catalogo360 en el Postgres local
```

## Funcionalidad

### Pedidos

Estados: `CREADO → ACEPTADO → EN_PREPARACION → DESPACHADO → ENTREGADO`, y `CANCELADO` (solo desde `CREADO`).

- Un pedido **no puede pasar a DESPACHADO sin haber sido ACEPTADO**.
- Al **aceptar** un pedido, el stock de cada producto disminuye en el catálogo. Si no hay stock suficiente, el cambio se rechaza.
- Nombre y precio de los productos se toman del catálogo, no de lo que envía el navegador.

### Catálogo

Listado de productos, creación y edición con precio y stock.

### Roles (App Roles de Entra ID)

| Acción | Admin | Operador | Cliente |
|---|:---:|:---:|:---:|
| Ver catálogo | Sí | Sí | Sí |
| Crear / editar productos | Sí | No | No |
| Crear pedido | Sí | Sí | Sí |
| Ver pedidos propios | Sí | Sí | Sí |
| Ver todos los pedidos | Sí | Sí | No |
| Cambiar estado de un pedido | Sí | Sí | No |
| Panel `/api/admin/*` | Sí | No | No |

## Endpoints

Todos van bajo `/api`. Solo `/api/publico/**` no requiere token.

| Método | Ruta | Roles |
|---|---|---|
| POST | `/api/orders` | Cliente, Operador, Admin |
| GET | `/api/orders` | Cliente, Operador, Admin |
| GET | `/api/orders/{id}` | Cliente, Operador, Admin |
| PUT | `/api/orders/{id}/status` | Operador, Admin |
| GET | `/api/catalog/products` | Cliente, Operador, Admin |
| POST | `/api/catalog/products` | Admin |
| PUT | `/api/catalog/products/{id}` | Admin |
| GET | `/api/admin/dashboard` | Admin |
| GET | `/api/publico/ping` | Público |

Los roles se leen del claim `roles` del token de Entra ID y se convierten a `ROLE_ADMIN`, `ROLE_OPERADOR` y `ROLE_CLIENTE`.

## Rutas de Angular

| Ruta | Acceso |
|---|---|
| `/login` | Público |
| `/dashboard` | Autenticado (contenido según rol) |
| `/orders` | Autenticado |
| `/catalog` | Autenticado |

## Configuración de Microsoft Entra ID

Los identificadores (no son secretos) están en:

- `frontend/src/environments/environment.ts` → `clientId`, `authority`, `redirectUri`, `apiScopes`
- `backend/*/src/main/resources/application.yml` → `issuer-uri`
- `backend/*/.../config/SecurityConfig.java` → `CLIENT_ID` esperado como audience

Requisitos en el tenant:

- App Registration de tipo **SPA** con Redirect URI `http://localhost:4200`.
- Scope expuesto `access_as_user`.
- App Roles `Admin`, `Operador` y `Cliente`, asignados a los usuarios de prueba.
- `accessTokenAcceptedVersion: 2` en el manifest (el issuer usado es v2.0).

> Este proyecto **no usa Client Secret**: una SPA con PKCE no lo necesita.

## Ejecutar en local

Requisitos: Docker y Docker Compose.

```bash
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| Angular | http://localhost:4200 |
| pedidos-service | http://localhost:8081 |
| catalogo-service | http://localhost:8082 |
| PostgreSQL | localhost:5432 |

Para correr un servicio con Maven en Windows (PowerShell):

```powershell
cd backend/pedidos-service
Copy-Item env-windows.example.ps1 env-windows.ps1   # y editar la contraseña local
./run-windows.ps1
```

## Despliegue en AWS

Región: `us-east-1`.

1. **Security Groups**
   - `dsy1107-ec2-sg`: SSH (22) solo desde tu IP; TCP 8081 y 8082 abiertos para el API Gateway.
   - `dsy1107-rds-sg`: PostgreSQL (5432) con origen `dsy1107-ec2-sg`.
2. **RDS PostgreSQL** (sin acceso público) con base inicial `pedidos360`. Luego crear la segunda base:
   ```bash
   psql -h <ENDPOINT_RDS> -U postgres -d pedidos360 -c "CREATE DATABASE catalogo360;"
   ```
3. **EC2** (Amazon Linux 2023, t3.small o superior) con Elastic IP. Instalar Docker, el plugin de Compose y Git.
4. **Levantar los servicios** en la EC2:
   ```bash
   git clone <URL_DEL_REPO> && cd <repo>
   cat > .env <<'EOF'
   DB_HOST=<ENDPOINT_RDS>
   DB_USER=postgres
   DB_PASSWORD=<password>
   CORS_ORIGINS=http://localhost:4200
   EOF
   docker compose -f docker-compose.aws.yml --env-file .env up -d --build
   ```
5. **API Gateway**: editar `EC2_HOST` en `aws/crear-api-gateway.sh` y ejecutarlo en AWS CloudShell. Al final imprime la URL del Gateway.
6. **Frontend**: pegar esa URL en `GATEWAY_URL` de `frontend/src/environments/environment.ts` (termina en `/api`).

## Pruebas de seguridad (200 / 401 / 403)

| Escenario | Resultado |
|---|---|
| Token válido con permiso (ej. Cliente lista productos) | **200** |
| Sin token o token inválido | **401** (lo rechaza el JWT Authorizer del Gateway) |
| Token válido sin el rol requerido (ej. Cliente hace `POST /api/catalog/products`) | **403** (lo rechaza Spring Security) |

```bash
# Sin token → 401
curl -i https://<ID>.execute-api.us-east-1.amazonaws.com/api/orders

# Ruta pública → 200
curl -i https://<ID>.execute-api.us-east-1.amazonaws.com/api/publico/ping

# Con token → 200 / 403 según el rol
curl -i -H "Authorization: Bearer <TOKEN>" https://<ID>.execute-api.us-east-1.amazonaws.com/api/catalog/products
```

## Seguridad del repositorio

- No hay contraseñas, Client Secret ni cadenas de conexión con credenciales en el código.
- Las credenciales de base de datos se entregan por variables de entorno (`DB_HOST`, `DB_USER`, `DB_PASSWORD`).
- `.gitignore` excluye `.env`, `env-windows.ps1`, `*.pem`, `*.key`, `node_modules/` y `target/`.

## Limitaciones conocidas

- Los puertos 8081 y 8082 de la EC2 están abiertos a internet porque el API Gateway HTTP API no tiene IP fija; Spring Security valida el JWT igualmente. En producción se usaría una integración privada (VPC Link).
- El frontend se ejecuta en `localhost:4200`; no está desplegado en la nube.
- Notificaciones (RabbitMQ), reportería y auditoría (Kafka) se incorporan en evaluaciones posteriores.
