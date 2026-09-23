export const environment = {
  production: false,

  msalConfig: {
    clientId: 'CAMBIA-ESTO-CLIENT-ID-FRONTEND',
    authority: 'https://login.microsoftonline.com/CAMBIA-ESTO-TENANT-ID',
    redirectUri: 'http://localhost:4200',
  },

  apiScopes: ['api://CAMBIA-ESTO-CLIENT-ID-BACKEND/access_as_user'],

  // Local / Docker Compose: cada microservicio en su propio puerto.
  // Cuando despliegues en AWS, reemplaza esto por la URL del API Gateway
  // real de AWS, que internamente redirige a cada microservicio en EC2.
  pedidosApiUrl: 'http://localhost:8081/api',
  catalogoApiUrl: 'http://localhost:8082/api',
};
