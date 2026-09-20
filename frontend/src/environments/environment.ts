export const environment = {
  production: false,

  msalConfig: {
    clientId: 'CAMBIA-ESTO-CLIENT-ID-FRONTEND',
    authority: 'https://login.microsoftonline.com/CAMBIA-ESTO-TENANT-ID',
    redirectUri: 'http://localhost:4200',
  },

  // Scope del backend (cualquiera de los microservicios que hayas expuesto
  // como API en Entra ID; si compartes audience entre servicios, uno basta)
  apiScopes: ['api://CAMBIA-ESTO-CLIENT-ID-BACKEND/access_as_user'],

  // Todo el trafico va al API Gateway, nunca directo a un microservicio
  apiBaseUrl: 'http://localhost:8080/api',
};
