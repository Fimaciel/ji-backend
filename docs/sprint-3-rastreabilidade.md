# Rastreabilidade da Sprint 3

Fonte funcional: Projeto de Software do TCC1, em `contextos/software.pdf`.
Período planejado: 29/09/2026 a 12/10/2026, com 21 horas. Execução registrada
em 03/11/2026. Escopo implementado no `ji-back` e no `ji-front`: RF-06,
RF-07, RF-11, RF-12 e RF-14. Esta matriz não altera a documentação do TCC2.

## Requisitos e regras de negócio

| Requisito | Evidência no back-end | Evidência no front-end | Verificação automatizada | Situação |
|---|---|---|---|---|
| RF-06 — Excluir entrada/saída | `DELETE /api/v1/transacoes/{id}`; isolamento por contexto; evento `EXCLUSAO_TRANSACAO` | confirmação antes da exclusão em `/app/transacoes` | `cadastraListaFiltraEditaEExcluiTransacoes`; Vitest e Playwright | Coberto |
| RF-07 — Listar entrada/saída | `GET /api/v1/transacoes`; página fixa de 20 itens; ordenação por data e criação | tabela responsiva, estado vazio e paginação | `paginaDeTransacoesTemVinteItens`; testes de componente e E2E | Coberto |
| Filtros da listagem | filtros por período, tipo, categoria e busca por descrição/categoria | controles de busca, período, tipo e categoria | teste integrado dos filtros; `ServicoFinanceiroMock.test.ts`; `financeiro.spec.ts` | Coberto |
| RF-11 — Cadastrar entrada/saída | `POST /api/v1/transacoes`; valor positivo; data não futura; descrição e categoria obrigatórias | modal com React Hook Form e Zod, valor em pt-BR e mensagens por campo | `validaDadosArquivaCategoriaUsadaEIsolaContextos`; Vitest, Playwright mock e REST integrado | Coberto |
| RF-12 — Categorizar transações | `GET/POST/PUT/DELETE /api/v1/categorias`; cinco categorias padrão; personalizadas por contexto | tela `/app/categorias`; padrão protegida e personalizada editável | `gerenciaCategoriasPersonalizadasSemAlterarAsPadrao`; testes mock, componente e E2E | Coberto |
| Exclusão de categoria utilizada | exclusão lógica com `data_exclusao`; histórico preserva a referência; categoria deixa de aceitar novos lançamentos | confirmação explica a preservação dos registros antigos | `validaDadosArquivaCategoriaUsadaEIsolaContextos`; teste do mock | Coberto |
| RF-14 — Editar entrada/saída | `PUT /api/v1/transacoes/{id}` restrito ao contexto ativo | formulário preenchido com os dados atuais | `cadastraListaFiltraEditaEExcluiTransacoes`; Vitest e Playwright | Coberto |
| Contexto ativo e permissão de escrita | `ContextoAtivoService` exige participação ativa e papel com escrita | serviços usam a sessão autenticada; nenhuma seleção de contexto é simulada no formulário | teste de isolamento entre duas contas; fluxo REST autenticado | Coberto |
| Auditoria aplicável | criação, edição e exclusão de categoria; exclusão de transação | mensagens de retorno sem expor detalhes internos | asserções em `TransacoesCategoriasApiIntegrationTest` | Coberto |

## Banco e contrato

A migração `V3__sprint_3_transacoes_categorias.sql` cria `categoria` e
`transacao`, adiciona os índices de consulta e insere Alimentação, Transporte,
Saúde, Lazer e Salário. Restrições e gatilho no PostgreSQL reforçam valor
positivo, tipo válido e uso de categoria padrão ou do mesmo contexto. A
aplicação também valida a data não futura e repete as demais regras para
devolver erros em
`application/problem+json` com códigos estáveis.

## Resultado da verificação

- `mvn clean verify`: aprovado; 40 testes, sem falhas; Flyway V1 a V3 em
  PostgreSQL 17 via Testcontainers;
- cobertura JaCoCo: 96,21% de linhas, 93,60% de instruções e 78,72% de ramos;
- imagem da API construída e executada pelo Compose; health check `UP`;
- `npm run lint`, `npm run typecheck` e `npm run build`: aprovados;
- `npm test`: 38 testes Vitest aprovados;
- `npm run test:e2e`: 33 cenários aprovados em Chromium 1440 px, Firefox
  820 px e WebKit 390 px; os três cenários REST foram ignorados nesse modo;
- `npm run test:e2e:integrado`: três cenários aprovados contra API,
  PostgreSQL e Mailpit reais, incluindo criação de categoria e transação;
- inspeção visual realizada nas larguras de referência, com correção da marca
  e da navegação no viewport de 390 px.

Os relatórios locais ficam em `ji-back/target/site/jacoco/index.html` e
`ji-front/playwright-report/index.html`. Nenhum commit ou push faz parte desta
entrega.
