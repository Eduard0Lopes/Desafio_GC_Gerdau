# Front-end — Vue 3 + Vite

```bash
npm ci
npm run dev     # http://localhost:5173, proxy /api -> http://localhost:8080
npm run build
```

Configuração (`.env.local`, ver `.env.example`): `VITE_API_URL` (padrão `/api`) e `VITE_PROXY_TARGET` (padrão `http://localhost:8080`).

Telas: resumo, códigos completos (filtros, prévia do ID, serviço existente ou novo), códigos de serviço
(paginação e alerta de duplicidade enquanto digita), grupos, fornecedores e unidades (CRUD).
Estrutura descrita em [docs/ARQUITETURA.md](../../docs/ARQUITETURA.md#front-end--estrutura).
