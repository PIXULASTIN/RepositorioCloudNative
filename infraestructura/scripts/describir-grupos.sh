#!/bin/bash
# Lista los consumer groups o muestra offsets, lag y consumidores de uno.
#   Uso:  bash infraestructura/scripts/describir-grupos.sh                    (lista todos)
#         bash infraestructura/scripts/describir-grupos.sh reporteria-pedidos (detalle + lag)
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka1:29092,kafka2:29093,kafka3:29094}"
GRUPO="${1:-}"

if [ -n "$GRUPO" ]; then
  docker exec kafka1 kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$GRUPO"
else
  docker exec kafka1 kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --list
fi
