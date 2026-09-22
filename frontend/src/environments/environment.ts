export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  apiBaseUrl: 'http://localhost:8080/api',
  msalConfig: {
    clientId: '066465b6-fb1c-4ec7-885f-09f30d0d9272',
    authority: 'https://login.microsoftonline.com/72e6a071-a2c0-48df-8b97-67eee4989d88',
    redirectUri: 'http://localhost:4200'
  },
  apiScopes: ['api://066465b6-fb1c-4ec7-885f-09f30d0d9272/access_as_user']
};