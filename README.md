# Pedidos360

Sistema de gestión de pedidos y catálogo de productos con stock, construido como proyecto de **DSY1107 — Desarrollo Cloud Native I**.

**Integrantes:**  Agustin Llanten, Ignacio Perez

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
│   ├── pedidos-service/     # Pedidos y estados (puerto 8081) + producer/consumers RabbitMQ
│   ├── catalogo-service/    # Productos, precios y stock (puerto 8082)
│   ├── rabbit-admin-service/# Administrador REST de RabbitMQ (puerto 8083)  [EP3]
│   ├── kafka-admin-service/ # Administrador REST de Kafka: tópicos, configs, grupos y lag (puerto 8084)  [EP5]
│   └── reporteria-service/  # Consumidor Kafka: reportería en tiempo real (puerto 8085)  [EP5]
├── frontend/                # Angular + MSAL
├── aws/
│   └── crear-api-gateway.sh # Crea el API Gateway (JWT + CORS + rutas)
├── rabbitmq/
│   └── init-cluster.sh      # Une el 2.º nodo al clúster y aplica policies TTL/max-length  [EP3/EP4]
├── infraestructura/         # [EP5/EP6]
│   ├── docker-compose-kafka.yml      # 3 ZooKeeper + 3 brokers + creación de tópicos
│   ├── docker-compose-kafka-ui.yml   # Kafka UI (http://localhost:8090)
│   ├── docker-compose-rabbitmq.yml   # Clúster RabbitMQ de 2 nodos
│   └── scripts/                      # crear-topicos.sh, describir-topicos.sh, describir-grupos.sh
├── docs/
│   └── EP3_EP4_RabbitMQ.md  # Guía de RabbitMQ: topología, pruebas, despliegue y evidencias
├── .env.example            # Variables que debe tener el .env de la EC2 (sin valores reales)
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
| POST | `/api/admin/mensajeria/prueba` | Admin (mensaje de prueba a RabbitMQ) |
| POST / GET / DELETE | `/api/rabbit/queues`, `/api/rabbit/queues/{name}` | Admin |
| POST / DELETE | `/api/rabbit/exchanges`, `/api/rabbit/exchanges/{name}` | Admin |
| POST / DELETE | `/api/rabbit/bindings` | Admin |
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
| rabbit-admin-service | http://localhost:8083 |
| RabbitMQ Management UI | http://localhost:15672 (guest / guest) |
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
   - `dsy1107-ec2-sg`: SSH (22) solo desde tu IP; TCP 8081, 8082 y 8083 abiertos para el API Gateway; 15672/15673 (Management UI de RabbitMQ) solo desde tu IP.
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
   RABBITMQ_USER=<usuario>
   RABBITMQ_PASSWORD=<password>
   RABBITMQ_ERLANG_COOKIE=<cadena-aleatoria-larga>
   EOF
   docker compose -f docker-compose.aws.yml --env-file .env up -d --build
   ```
5. **API Gateway**: editar `EC2_HOST` en `aws/crear-api-gateway.sh` y ejecutarlo en AWS CloudShell. Al final imprime la URL del Gateway.
6. **Frontend**: pegar esa URL en `GATEWAY_URL` de `frontend/src/environments/environment.ts` (termina en `/api`).

## Mensajería asíncrona con RabbitMQ (EP3 + EP4)

Al aceptar o despachar un pedido, `pedidos-service` publica un evento en RabbitMQ (después del commit). Los consumers lo procesan con ACK/NACK explícito y los mensajes que fallan van a una DLQ.

```
PedidoService → PedidoEventPublisher
   ├─ pedidos.topic  (topic)  --pedido.*--------------> pedidos.notificaciones.queue --(falla)--> .dlq
   └─ pedidos.direct (direct) --pedido.aceptado-------> pedidos.aceptados.queue      --(falla)--> .dlq
                              --pedido.despachado-----> pedidos.despachos.queue      --(falla)--> .dlq
```

- **Configuración centralizada:** `app.rabbit.*` en `application.yml` + `RabbitTopologyConfig` (beans `Queue`, `Exchange`, `Binding`), separada de la lógica de negocio.
- **Consumers** (`@RabbitListener`) agrupados por dominio: `PedidoConsumer` y `NotificacionConsumer`.
- **Administrador:** `rabbit-admin-service` (`/api/rabbit/...`, rol Admin) con `RabbitAdminService` y validación de parámetros.
- **Clúster cloud:** `docker-compose.aws.yml` levanta `rabbit1` + `rabbit2`; `rabbit-init` los une y aplica las policies de TTL / max-length.

Pruebas, despliegue del clúster y checklist de evidencias: [`docs/EP3_EP4_RabbitMQ.md`](docs/EP3_EP4_RabbitMQ.md).

## Streaming de eventos con Apache Kafka (EP5 + EP6)

Kafka **complementa** a RabbitMQ y no lo reemplaza: RabbitMQ sigue encargándose de las colas de trabajo (aceptados, despachos, notificaciones con DLQ) y Kafka transporta el flujo de eventos de pedidos y stock para reportería en tiempo real.

### Arquitectura

| Servicio | Puerto | Rol con Kafka |
|---|---|---|
| `pedidos-service` | 8081 | Productor en `pedidos.eventos`. Consumidor de `catalogo.stock` (grupo `pedidos-stock`): alerta de stock bajo. |
| `catalogo-service` | 8082 | Productor en `catalogo.stock` cuando cambia el stock de un producto. |
| `reporteria-service` | 8085 | Consumidor de `pedidos.eventos` (grupo `reporteria-pedidos`, 3 hilos) y de `catalogo.stock` (grupo `reporteria-stock`). `GET /api/reportes/resumen`. |
| `kafka-admin-service` | 8084 | Administra tópicos, configuraciones, consumer groups y lag con `AdminClient` (rol ADMIN). |

| Tópico | Key | Particiones / réplicas | Configuración |
|---|---|---|---|
| `pedidos.eventos` | `pedidoId` | 3 / 3 | `retention.ms` 7 días, `cleanup.policy=delete`, `min.insync.replicas=2` |
| `catalogo.stock` | `productoId` | 3 / 3 | `cleanup.policy=compact`, `min.insync.replicas=2` |
| `*.DLT` | — | 3 / 3 | Mensajes con error, retención 30 días |

Manejo de errores en los consumidores: `ErrorHandlingDeserializer` (un mensaje corrupto no detiene al consumidor), validación del evento, reintentos con backoff exponencial y envío a `<tópico>.DLT`. Si Kafka falla al publicar, se registra en el log y no afecta al pedido ni a RabbitMQ.

### Levantar Kafka

```bash
docker network create pedidos360-net          # una sola vez
docker compose -f infraestructura/docker-compose-kafka.yml up -d
docker compose -f infraestructura/docker-compose-kafka-ui.yml up -d     # http://localhost:8090
docker logs kafka-init                         # crea los 2 tópicos + los 2 .DLT
```

Servicios en Docker (usan la red `pedidos360-net`): `docker compose up -d --build`. Servicios desde el IDE: usan por defecto `localhost:9092,9093,9094` (variable `KAFKA_BOOTSTRAP_SERVERS`).

### Pruebas rápidas

```bash
# Publicar un evento de prueba (rol ADMIN)
curl -X POST http://localhost:8081/api/admin/kafka/prueba -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"pedidoId":1,"estado":"ACEPTADO"}'

# Reporte construido desde Kafka
curl http://localhost:8085/api/reportes/resumen -H "Authorization: Bearer $TOKEN"

# Administración (rol ADMIN)
curl http://localhost:8084/api/kafka/topics -H "Authorization: Bearer $TOKEN"
curl http://localhost:8084/api/kafka/consumer-groups/reporteria-pedidos/lag -H "Authorization: Bearer $TOKEN"

# Mensaje inválido directo al tópico: va a pedidos.eventos.DLT y el consumidor sigue funcionando
echo "no-es-json" | docker exec -i kafka1 kafka-console-producer --bootstrap-server kafka1:29092 --topic pedidos.eventos
```

Endpoints de `kafka-admin-service`: `GET /api/kafka/cluster`, `GET|POST /api/kafka/topics`, `GET|DELETE /api/kafka/topics/{name}`, `PUT /api/kafka/topics/{name}/config`, `PUT /api/kafka/topics/{name}/partitions`, `GET /api/kafka/consumer-groups`, `GET|DELETE /api/kafka/consumer-groups/{id}`, `GET /api/kafka/consumer-groups/{id}/lag`. Los tópicos funcionales y los `.DLT` están protegidos contra eliminación.

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
- Kafka (3 ZooKeeper + 3 brokers) necesita ~3 GB de RAM: se ejecuta en el computador local con Docker Desktop, no en la EC2 `t3.small`. Los servicios en la EC2 funcionan sin Kafka (los eventos se descartan con un log de error).
- `reporteria-service` mantiene sus métricas en memoria: al reiniciarse reinicia los contadores (los offsets del grupo se conservan en Kafka).
- Los tópicos `.DLT` (Dead Letter Topic) son técnicos, para mensajes que no se pudieron procesar; los dos tópicos funcionales son `pedidos.eventos` y `catalogo.stock`.
