
# Requiere que ambos nodos compartan el mismo RABBITMQ

set -euo pipefail

if [ -z "${RABBIT_SECOND_NODE:-}" ]; then
  echo "[init-cluster] RABBIT_SECOND_NODE no definido (entorno de 1 solo nodo). Nada que hacer."
  exit 0
fi

SEGUNDO_HOST="${RABBIT_SECOND_NODE#*@}"

echo "[init-cluster] Esperando a que $RABBIT_SECOND_NODE (host: $SEGUNDO_HOST) responda..."
intentos=30
hasta_listo=0
for i in $(seq 1 "$intentos"); do
  if rabbitmqctl -n "$RABBIT_SECOND_NODE" status >/dev/null 2>&1; then
    hasta_listo=1
    break
  fi
  echo "  intento $i/$intentos..."
  sleep 2
done

if [ "$hasta_listo" -ne 1 ]; then
  echo "[init-cluster] ERROR: $RABBIT_SECOND_NODE nunca respondio."
  exit 1
fi

# Si existe en cluster (reinicios, re-despliegues), no falla: solo lo informa.
if rabbitmqctl -n "$RABBIT_SECOND_NODE" cluster_status 2>/dev/null | grep -q "$RABBIT_MAIN_NODE"; then
  echo "[init-cluster] $RABBIT_SECOND_NODE ya esta en el cluster de $RABBIT_MAIN_NODE. Nada que hacer."
  exit 0
fi

echo "[init-cluster] Uniendo $RABBIT_SECOND_NODE al cluster de $RABBIT_MAIN_NODE..."
rabbitmqctl -n "$RABBIT_SECOND_NODE" stop_app
rabbitmqctl -n "$RABBIT_SECOND_NODE" join_cluster "$RABBIT_MAIN_NODE"
rabbitmqctl -n "$RABBIT_SECOND_NODE" start_app

echo "[init-cluster] Cluster formado:"
rabbitmqctl -n "$RABBIT_SECOND_NODE" cluster_status
