#!/usr/bin/env bash
# Abre o aplicativo desktop. Requer JDK 21+ e Maven. A API precisa estar no ar (docker compose up).
set -euo pipefail
cd "$(dirname "$0")"
export API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
exec mvn -q javafx:run
