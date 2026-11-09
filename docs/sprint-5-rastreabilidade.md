# Rastreabilidade da Sprint 5

Período planejado no Projeto de Software: 27/10/2026 a 09/11/2026. Execução
real: 08/11/2026. Esta entrega implementa contextos compartilhados, seleção do
contexto ativo e resposta a convites; o envio e o gerenciamento de
participantes pertencem à Sprint 6.

| Requisito | API e implementação | Evidência automatizada | Situação |
|---|---|---|---|
| RF-17 — Criar contexto | `POST /api/v1/contextos`; `ContextoService.criar` | `ContextosConvitesApiIntegrationTest.criaListaESelecionaContextoCompartilhado` | Coberto |
| NF17.2–NF17.5 | nome obrigatório; tipo `COMPARTILHADO`; criador `ADMINISTRADOR`; datas e auditoria | Flyway V5 e teste integrado com PostgreSQL | Coberto |
| RF-21 — Selecionar contexto ativo | `GET /api/v1/contextos`; `PUT /api/v1/contextos/ativo`; sessão e login sinalizam seleção necessária | criação, listagem, seleção e rejeição de operação sem contexto no teste integrado | Coberto |
| NF21.1–NF21.4 | seleção automática quando há um contexto; escolha obrigatória quando há mais de um; persistência em `sessao.contexto_id` | testes da autenticação e `ContextosConvitesApiIntegrationTest` | Coberto |
| RF-19 — Aceitar ou recusar convite | `GET /api/v1/convites`, consulta pública por token, aceite e recusa autenticados | `aceitaERecusaConvitesPendentesComUsoUnico` | Coberto para colaborador |
| NF19.1–NF19.4 | somente destinatário; expiração; resposta única; participação criada no aceite; auditoria | teste integrado com convites pendentes, aceitos e recusados | Coberto |
| NF19.2 — dependente Leitura | aceite vinculado ao cadastro de dependente | implementação programada com RF-20 | Sprint 6 |

## Contrato entregue

| Método | Rota | Resultado principal |
|---|---|---|
| `GET` | `/api/v1/contextos` | contextos ativos do usuário e indicação do atual |
| `POST` | `/api/v1/contextos` | cria contexto compartilhado e participação administrativa |
| `PUT` | `/api/v1/contextos/ativo` | persiste o contexto da sessão |
| `GET` | `/api/v1/convites` | lista convites do e-mail autenticado |
| `GET` | `/api/v1/convites/token/{token}` | mostra convite válido sem autenticação |
| `POST` | `/api/v1/convites/{id}/aceite` | cria participação e consome o convite |
| `POST` | `/api/v1/convites/{id}/recusa` | recusa e consome o convite |

Todas as mutações validam a participação ativa e gravam auditoria. O token do
convite é persistido somente como SHA-256. A interface cobre seleção após o
login, seletor fixo, criação de contexto, lista de convites e resposta por
lista ou link.
