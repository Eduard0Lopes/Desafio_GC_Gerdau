#!/usr/bin/env sh
# ============================================================================
#  Desafio GC - Gerdau: sobe SEM DOCKER (backend Java + frontend Node)
#
#  Use este script em maquinas que NAO tem Docker. Roda de qualquer pasta.
# ============================================================================
cd "$(dirname "$0")" || exit 1

# 1) pre-requisitos instalados na maquina
command -v java >/dev/null 2>&1 || {
    echo "[ERRO] Java nao encontrado (e preciso Java 17 ou superior)."
    echo "       Instale em https://adoptium.net e rode ./dev.sh de novo."
    exit 1
}
command -v node >/dev/null 2>&1 || {
    echo "[ERRO] Node.js nao encontrado (e preciso Node 20 ou superior)."
    echo "       Instale em https://nodejs.org e rode ./dev.sh de novo."
    exit 1
}

# 2) dependencias do frontend (instala so na primeira vez)
if [ ! -d JAVA/frontend/node_modules ]; then
    echo "Instalando as dependencias do frontend (primeira vez, pode demorar)..."
    (cd JAVA/frontend && npm install --no-audit --no-fund) || exit 1
fi

# 3) sobe backend e frontend juntos
echo "Iniciando:"
echo "  Backend  > http://localhost:8080"
echo "  Frontend > http://localhost:5173  (abra esta no navegador)"
(cd JAVA/backend && ./mvnw spring-boot:run) &
BACK=$!
(cd JAVA/frontend && npm run dev) &
FRONT=$!
trap 'kill $BACK $FRONT 2>/dev/null' INT TERM EXIT
wait
