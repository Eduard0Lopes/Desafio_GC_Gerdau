# Roteiro de Demonstração (apresentação)

Sugestão de fluxo de ~8 minutos para a apresentação.

## Preparação (antes de começar)

```bash
docker compose up --build
```

Abrir **http://localhost**. Em `Perfil`, deixar **Usuário** (visão do comprador).

## 1. O problema (1 min)

Mostrar em **Governança** (trocar para Admin): os **4.384 registros** do histórico
com decisão DUPLICIDADE/ALERTA — "é isto que existe hoje na base".

## 2. A base saneada (1 min)

- Menu do Admin → fila histórica: item com **95% Match** e a descrição legada
  (ex.: `CS-130835 | SV-EQUIPE TELHADO | legado 4033776`).
- Isto é o merge oficial: `base_saneada.xlsx` × `matriz_de_para_gerdau.xlsx`.

## 3. Consulta com fuzzy (1 min) — na hora de criar

- **Cadastrar Novo Item** → Tipo "Código de Serviço" → digitar
  `SV EQUIPE TELHADO` → a checagem automática mostra os semelhantes com `% Match`
  e o contador `n/40 Caracteres SAP`.

## 4. O filtro anti-duplicidade (2 min) — o coração do desafio

- Na tela inicial, montar uma composição com um serviço parecido → **Vincular Serviço**.
- O **Bloqueio de Duplicidade** aparece (print): `ID: … | … | 94% Match`.
- Demonstração das três saídas:
  1. **Reutilizar Código Existente** (recomendado);
  2. **Enviar para Governança** (justificativa + validação humana);
  3. Voltar e criar algo realmente novo → decide **PERMITIR** e vincula.

## 5. Governança e permissões (2 min)

- Trocar o perfil para **Usuário**: o menu de Governança **desaparece** (o backend
  também responde **403** — segurança no servidor, não só na interface).
- Voltar para **Admin**: aprovar/revisar um item com observação e mostrar que o
  registro guarda **quem decidiu, quando e por quê** (`reviewedBy`/`reviewedAt`).

## 6. Encerramento (1 min)

- Números: **9.795 registros legados → 7.308 códigos canônicos (25,4% de redução)**,
  rastreabilidade 100% (de-para), 4.384 pendências em análise.
- Swagger (`/swagger-ui.html`) como porta de integração para o SAP.

## Verbal de segurança

Se algo falhar: a página mostra o estado (backend indisponível etc.); recarregar
resolve a maioria dos casos. O banco é recriado do zero com
`docker compose down -v && docker compose up --build`.
