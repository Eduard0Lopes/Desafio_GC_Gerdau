# Permissões e Perfis

Controle de acesso conforme a seção **12.2** do documento técnico:
*"Ocultar um botão na interface não é controle de segurança"* — as regras valem
também (e principalmente) **no servidor**.

## Perfis

| Perfil | O que pode fazer | Observação |
|---|---|---|
| **USUARIO** (padrão) | consultar catálogos, verificar similaridade, cadastrar itens, vincular código completo (fluxo com bloqueio/justificativa/envio à governança) | comprador/solicitante |
| **ADMIN** | tudo o que o usuário faz **+ esteira de governança**: fila de validação, aprovar/rejeitar novos cadastros, revisar o histórico da matriz, auditoria de exceções **+ exclusão de componentes do catálogo** (unitária e em massa) | admin de dados |

## Como funciona nesta versão (demonstração)

### Frontend
- Seletor **Perfil: Usuário | Admin** na barra superior (persistido em `localStorage`).
- Com perfil **Usuário**: a interface é minimalista — as telas dos fluxos (vinculação e
  cadastro) e o **Catálogo** (somente leitura); o link de Governança **não aparece**.
- Com perfil **Admin**: o menu exibe também **Painel Admin** (com o contador de
  pendências) e o **Catálogo** ganha checkboxes para exclusão em massa.

### Backend (a garantia real)
- Todo endpoint em `/api/v1/governanca/**` exige o header
  `X-Usuario-Papel: ADMIN` — sem ele, a resposta é **403 Forbidden** mesmo que
  a chamada venha de fora do frontend (curl, Swagger, outra aplicação).
- `DELETE` em componentes do catálogo, `POST /api/v1/catalogo/exclusao-em-massa`,
  `POST /api/v1/catalogo/situacao` (desativar/reativar) e `POST /api/v1/catalogo/importacao`
  também exigem `ADMIN`. A interface usa **desativar/reativar** (soft delete: o item sai de
  circulação, mas nada é perdido e ele pode voltar); a exclusão definitiva continua na API,
  bloqueada enquanto houver vínculo.
- Os demais endpoints são abertos ao perfil USUARIO nesta versão.
- Headers de identidade:

| Header | Exemplo | Uso |
|---|---|---|
| `X-Usuario-Id` | `403921` | identifica o solicitante (auditoria, reviewedBy) |
| `X-Usuario-Papel` | `ADMIN` ou `USUARIO` | autorização no servidor |

```bash
# bloqueado para usuário comum:
curl http://localhost:8080/api/v1/governanca/resumo \
  -H "X-Usuario-Papel: USUARIO"      # → 403 Forbidden

# liberado para admin:
curl http://localhost:8080/api/v1/governanca/resumo \
  -H "X-Usuario-Papel: ADMIN"        # → 200 OK
```

## Evolução prevista (seção 13 — Segurança)

Esta versão usa headers para a demonstração. O documento prevê:

1. **Autenticação corporativa OAuth 2.0/OpenID Connect** — o backend deriva o papel
   do **token autenticado**, nunca de um header livre;
2. **RBAC completo** com os quatro perfis do documento (Consultor, Solicitante,
   Administrador de dados, Administrador técnico);
3. HTTPS, rate limiting e tratamento de dados pessoais (LGPD) conforme a seção 13.

A implementação atual já está estruturada para essa troca: basta substituir a leitura
do header em `AutorizacaoInterceptor` pela identidade do token — os endpoints e a
interface não mudam.
