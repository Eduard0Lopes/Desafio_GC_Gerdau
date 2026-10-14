# Gerdau SCSS — integração

Aplicação para padronização de descrições/códigos de serviços SAP, com frontend Vue 3 + Vite, backend Spring Boot e MongoDB. A pasta `IA/` contém os artefatos de processamento e similaridade da equipe de IA & Dados.

## Componentes
- `frontend/`: Vue 3, Vite e Axios.
- `backend/desafio/`: Java/Spring Boot, DTOs, validação e persistência MongoDB.
- `../IA/`: scripts de preparação de dados e motor fuzzy.

## Contratos principais
| Recurso | Request |
|---|---|
| Grupo de serviço | `nome`, `codigoOriginal` opcional |
| Unidade de medida | `sigla`, `nome`, `codigoOriginal` opcional |
| Código de serviço | `descricao` (1–40 caracteres), `codigoOriginal` opcional, `grupoServicoId`, `unidadeMedidaId` |

A API responde com IDs de referência para grupo/unidade. O frontend deve resolver os nomes para exibição a partir das listas carregadas. Não enviar objetos aninhados quando o DTO espera apenas IDs.

## Execução local
1. Instale JDK e Node.js compatíveis com os manifests do projeto.
2. Configure a URI MongoDB em `backend/desafio/src/main/resources/application.properties` (arquivo intencionalmente não alterado nesta refatoração).
3. Backend: `cd backend/desafio` e execute `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`), se o wrapper estiver presente; caso contrário, use Maven instalado.
4. Frontend: `cd frontend`, `npm ci`, `npm run dev`.
5. No desenvolvimento, o Vite encaminha chamadas `/api` para o backend local conforme `vite.config.js`.

## Checklist de validação antes de produção
- [ ] Build/testes Maven com MongoDB de teste.
- [ ] `npm ci && npm run build`.
- [ ] CRUD de grupos, unidades e serviços.
- [ ] Validação de descrição vazia, 40 e 41 caracteres.
- [ ] Validar existência dos IDs de grupo/unidade no backend.
- [ ] Testar códigos completos e códigos de fornecedores.
- [ ] Confirmar o contrato de entrada/saída do fuzzy com IA & Dados e testar casos conhecidos.
- [ ] Subir o ambiente a partir de checkout limpo usando Docker Compose.
- [ ] Não versionar segredos; rotacionar credenciais reais que já tenham sido publicadas.

## Limites desta entrega
A documentação e os contratos foram alinhados ao código inspecionado. A aprovação final depende de executar builds/testes e comprovar a integração fuzzy ponta a ponta com o catálogo e os limiares oficiais de IA & Dados.
