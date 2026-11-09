# Rastreabilidade da Sprint 6

Período planejado no Projeto de Software: 10/11/2026 a 23/11/2026. Execução
real antecipada: 08/11/2026. Esta entrega implementa RF-18, RF-20 e RF-22 no
back-end e no front-end, sem alterar a documentação acadêmica do TCC.

| Requisito | API e implementação | Evidência automatizada | Situação |
|---|---|---|---|
| RF-18 — Convidar colaborador | envio, listagem, reenvio e cancelamento em `/api/v1/contextos/ativo/convites`; `GestaoConviteService` | `ColaboracaoApiIntegrationTest.enviaReenviaCancelaERecriaConviteDeColaborador`; `SmtpConviteMailSenderTest` | Coberto |
| NF18.1–NF18.5 | somente administrador de contexto compartilhado; papel `COLABORADOR`; validade de sete dias; bloqueio de convite pendente duplicado; token salvo somente como SHA-256 | teste integrado com PostgreSQL, capturador de e-mail e auditoria | Coberto |
| RF-20 — Cadastrar dependente | CRUD em `/api/v1/dependentes`; `DependenteService`; tabela `dependente` na Flyway V6 | `ColaboracaoApiIntegrationTest.cadastraDependentesEVinculaAcessoSomenteLeituraNoAceite` | Coberto |
| NF20.1–NF20.4 | tipos `PASSIVO` e `LEITURA`; e-mail obrigatório para leitura; convite com papel restrito; vínculo participante/dependente criado no aceite | teste de cadastro passivo, convite, aceite, acesso somente leitura e exclusão | Coberto |
| RF-22 — Gerenciar participantes e papéis | listagem histórica, alteração de papel, remoção e saída em `/api/v1/contextos/ativo/participantes`; `ParticipanteContextoService` | `ColaboracaoApiIntegrationTest.alteraPapeisPreservaHistoricoEProtegeUltimoAdministrador` | Coberto |
| NF22.1–NF22.5 | papéis administrativo/colaborador; dependente fora da conversão manual; `data_saida` preservada; revogação do contexto nas sessões; último administrador protegido | teste integrado de transferência, saída voluntária, histórico e conflito | Coberto |
| RNF-01 e RNF-09 | autorização por sessão/JWT, isolamento pelo contexto ativo, auditoria das mutações e nenhum token bruto persistido ou registrado | suíte integrada, Flyway V6 e adaptadores SMTP | Coberto |

## Contrato entregue

| Método | Rota | Resultado principal |
|---|---|---|
| `GET` | `/api/v1/contextos/ativo/convites` | histórico de convites enviados no contexto ativo |
| `POST` | `/api/v1/contextos/ativo/convites` | cria e envia convite de colaborador |
| `POST` | `/api/v1/contextos/ativo/convites/{id}/reenvio` | substitui o token e renova a validade |
| `DELETE` | `/api/v1/contextos/ativo/convites/{id}` | cancela convite pendente |
| `GET` | `/api/v1/contextos/ativo/participantes` | participantes ativos e histórico de saídas |
| `PATCH` | `/api/v1/contextos/ativo/participantes/{id}/papel` | alterna administrador/colaborador |
| `DELETE` | `/api/v1/contextos/ativo/participantes/{id}` | encerra a participação selecionada |
| `DELETE` | `/api/v1/contextos/ativo/participantes/me` | saída voluntária do contexto |
| `GET` | `/api/v1/dependentes` | dependentes ativos do contexto compartilhado |
| `POST` | `/api/v1/dependentes` | cadastra dependente e, em leitura, envia convite |
| `PUT` | `/api/v1/dependentes/{id}` | atualiza dados e convite pendente |
| `DELETE` | `/api/v1/dependentes/{id}` | exclusão lógica e encerramento do acesso vinculado |

## Verificação concluída

- `mvn clean verify`: 50 testes, Flyway V1 a V6 em PostgreSQL 17 e cobertura
  mínima de 60% aprovada;
- front-end: 48 testes Vitest, lint, typecheck, build e Playwright nos três
  tamanhos configurados;
- imagem Docker construída como `ji-back:sprint-6`.

A tela `/app/colaboracao` apresenta participantes, dependentes e convites em
áreas separadas. Ações destrutivas pedem confirmação e as opções
administrativas não são exibidas para colaboradores ou dependentes.
