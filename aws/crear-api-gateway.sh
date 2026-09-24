#!/usr/bin/env bash
# Crea el API Gateway (HTTP API) con JWT Authorizer de Microsoft Entra ID + CORS,
# apuntando a los dos microservicios que corren en la EC2.
# Ejecutar en AWS CloudShell:   bash crear-api-gateway.sh
set -euo pipefail

# ====== EDITA ESTAS VARIABLES ======
REGION="us-east-1"
EC2_HOST="PON_AQUI_IP_PUBLICA_O_DNS_DE_LA_EC2"
ORIGINS='["http://localhost:4200"]'      # origen(es) del frontend
TENANT_ID="72e6a071-a2c0-48df-8b97-67eee4989d88"
CLIENT_ID="066465b6-fb1c-4ec7-885f-09f30d0d9272"
# ===================================

API_ID=$(aws apigatewayv2 create-api --region "$REGION" --name pedidos360-api --protocol-type HTTP \
  --cors-configuration "{\"AllowOrigins\":$ORIGINS,\"AllowMethods\":[\"GET\",\"POST\",\"PUT\",\"DELETE\",\"OPTIONS\"],\"AllowHeaders\":[\"authorization\",\"content-type\"],\"MaxAge\":3600}" \
  --query ApiId --output text)
echo "API_ID=$API_ID"

# JWT Authorizer: valida firma, expiracion, issuer y audience del token de Entra ID.
# Se aceptan ambos formatos de audience (token v2 => GUID, token v1 => api://GUID).
AUTH_ID=$(aws apigatewayv2 create-authorizer --region "$REGION" --api-id "$API_ID" \
  --name entra-jwt --authorizer-type JWT --identity-source '$request.header.Authorization' \
  --jwt-configuration "{\"Audience\":[\"$CLIENT_ID\",\"api://$CLIENT_ID\"],\"Issuer\":\"https://login.microsoftonline.com/$TENANT_ID/v2.0\"}" \
  --query AuthorizerId --output text)
echo "AUTH_ID=$AUTH_ID"

integracion() {  # $1 = URL destino
  aws apigatewayv2 create-integration --region "$REGION" --api-id "$API_ID" \
    --integration-type HTTP_PROXY --integration-method ANY --payload-format-version 1.0 \
    --integration-uri "$1" --query IntegrationId --output text
}
ruta() {  # $1 = route-key, $2 = integration id, $3 = JWT | NONE
  if [ "$3" = "JWT" ]; then
    aws apigatewayv2 create-route --region "$REGION" --api-id "$API_ID" --route-key "$1" \
      --target "integrations/$2" --authorization-type JWT --authorizer-id "$AUTH_ID" >/dev/null
  else
    aws apigatewayv2 create-route --region "$REGION" --api-id "$API_ID" --route-key "$1" \
      --target "integrations/$2" --authorization-type NONE >/dev/null
  fi
  echo "ruta creada: $1 ($3)"
}

P="http://$EC2_HOST:8081"   # pedidos-service
C="http://$EC2_HOST:8082"   # catalogo-service

I=$(integracion "$P/api/orders");             ruta 'ANY /api/orders' "$I" JWT
I=$(integracion "$P/api/orders/{proxy}");     ruta 'ANY /api/orders/{proxy+}' "$I" JWT
I=$(integracion "$P/api/admin/{proxy}");      ruta 'ANY /api/admin/{proxy+}' "$I" JWT
I=$(integracion "$P/api/publico/{proxy}");    ruta 'ANY /api/publico/{proxy+}' "$I" NONE
I=$(integracion "$C/api/catalog/{proxy}");    ruta 'ANY /api/catalog/{proxy+}' "$I" JWT

# Preflight CORS: las rutas OPTIONS no llevan authorizer (el navegador no envia token en el preflight)
I=$(integracion "$P/api/publico/ping")
for R in 'OPTIONS /api/orders' 'OPTIONS /api/orders/{proxy+}' 'OPTIONS /api/admin/{proxy+}' 'OPTIONS /api/catalog/{proxy+}'; do
  ruta "$R" "$I" NONE
done
aws apigatewayv2 create-stage --region "$REGION" --api-id "$API_ID" --stage-name '$default' --auto-deploy >/dev/null

URL=$(aws apigatewayv2 get-api --region "$REGION" --api-id "$API_ID" --query ApiEndpoint --output text)
echo
echo "=========================================================="
echo "Listo. Pega esto en frontend/src/environments/environment.ts:"
echo "   const GATEWAY_URL = '$URL/api';"
echo "=========================================================="
