#!/usr/bin/env sh
# ============================================================================
#  Desafio GC - Gerdau: sobe TUDO com Docker (mongo + backend + frontend)
#
#  Sem Docker instalado? Use o ./dev.sh (modo nativo) - veja docs/COMO-RODAR.md.
#  Pode ser executado de QUALQUER lugar: o script sempre volta para a raiz
#  do repositorio antes de rodar o compose.
# ============================================================================
cd "$(dirname "$0")" || exit 1

# 1) o Docker precisa estar instalado e com o motor no ar
if ! command -v docker >/dev/null 2>&1; then
    echo "[ERRO] O Docker nao esta instalado nesta maquina."
    echo "       Sem Docker, use o ./dev.sh (modo nativo) - veja docs/COMO-RODAR.md."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "[ERRO] O Docker esta instalado, mas o motor nao esta rodando."
    echo "       Abra o Docker Desktop e execute este script de novo."
    echo "       Sem Docker? Use o ./dev.sh (modo nativo)."
    exit 1
fi

# 2) sobe tudo a partir da raiz do repositorio (onde esta o docker-compose.yml)
if docker compose version >/dev/null 2>&1; then
    docker compose up --build
else
    docker-compose up --build
fi
