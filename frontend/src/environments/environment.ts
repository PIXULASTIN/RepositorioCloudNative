// Pega aqui la URL de tu API Gateway (termina en /api). Ejemplo:
//   'https://abc123xyz.execute-api.us-east-1.amazonaws.com/api'
// Si la dejas vacia se usan los microservicios locales (docker compose / mvn spring-boot:run).
const GATEWAY_URL = '';

export const environment = {
  production: false,
  ordersApiUrl: GATEWAY_URL || 'http://localhost:8081/api',
  catalogApiUrl: GATEWAY_URL || 'http://localhost:8082/api',
  msalConfig: {
    clientId: '066465b6-fb1c-4ec7-885f-09f30d0d9272',
    authority: 'https://login.microsoftonline.com/72e6a071-a2c0-48df-8b97-67eee4989d88',
    redirectUri: 'http://localhost:4200'
  },
  apiScopes: ['api://066465b6-fb1c-4ec7-885f-09f30d0d9272/access_as_user']
};
