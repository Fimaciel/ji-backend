# Rastreabilidade da Sprint 4

Fonte funcional: Projeto de Software do TCC1, em `contextos/software.pdf`.
Período planejado: 13/10/2026 a 26/10/2026, com 12 horas. Execução registrada
em 06/11/2026. Escopo implementado no `ji-back` e no `ji-front`: RF-09 e
RF-16. Esta matriz não altera a documentação do TCC2.

## Requisitos e regras de negócio

| Requisito | Evidência no back-end | Evidência no front-end | Verificação automatizada | Situação |
|---|---|---|---|---|
| RF-09 — Dashboard | `GET /api/v1/dashboard?mes=AAAA-MM`; totais de entradas, saídas e saldo do contexto ativo | `/app` como primeira tela após o login, com faixa de indicadores mensais | `dashboardResumeMesComFluxoCategoriasRecentesEEstadoVazio`; testes de componente e Playwright | Coberto |
| Gráficos e resumos do RF-09 | agregação diária do fluxo e cinco categorias com maior saída | gráfico SVG acessível, barras por categoria e lista de transações recentes | integração PostgreSQL, `ServicoFinanceiroMock.test.ts` e `dashboard-orcamentos.spec.ts` | Coberto |
| Atualização em até dois segundos | resposta calculada diretamente das transações confirmadas, sem cache intermediário | consulta automática do dashboard a cada 2.000 ms | teste integrado do cálculo e fluxo REST completo | Coberto |
| Estado sem movimentações | totais zerados e coleções vazias para mês sem registros | orientação para cadastrar a primeira transação | `dashboardResumeMesComFluxoCategoriasRecentesEEstadoVazio` | Coberto |
| RF-16 — Definir orçamento por categoria | `GET/POST/PUT/DELETE /api/v1/orcamentos`; limite positivo e mês `AAAA-MM` | `/app/orcamentos`, formulário validado com React Hook Form e Zod | `criaListaEditaEExcluiOrcamentoComAlertasDeConsumo`; Vitest e Playwright | Coberto |
| Unicidade mensal | restrição única por contexto, categoria e mês, com validação anterior ao banco | categoria duplicada recebe mensagem no próprio campo | teste de conflito `orcamento-duplicado` e teste do mock | Coberto |
| Consumo e alertas | soma somente saídas da categoria no mês; `ATENCAO` em 80% e `EXCEDIDO` em 100% | medidores geral e individual com estados normal, atenção e excedido | testes com entrada ignorada, 80% e 106,67% de consumo | Coberto |
| Categoria disponível | aceita padrão ou personalizada ativa do contexto; gatilho V4 reforça o vínculo | seletor usa categorias disponíveis no contexto | `validaMesCategoriaEIsolaOrcamentosPorContexto` | Coberto |
| Contexto e permissão | leitura exige participação ativa; escrita exige `ADMINISTRADOR` ou `COLABORADOR` | todas as operações usam a sessão e o contexto ativos | isolamento entre duas contas no Testcontainers | Coberto |
| Auditoria | eventos `CRIACAO_ORCAMENTO`, `ATUALIZACAO_ORCAMENTO` e `EXCLUSAO_ORCAMENTO` | retorno em português sem detalhes internos | asserções em `OrcamentoDashboardApiIntegrationTest` | Coberto |

## Banco e contrato

A migração `V4__sprint_4_orcamentos.sql` cria `orcamento_categoria`, o índice
de consulta por contexto e mês e a unicidade por contexto, categoria e mês.
Uma restrição exige limite positivo e mês no primeiro dia; um gatilho impede
o uso de categoria personalizada excluída ou pertencente a outro contexto.
O consumo permanece derivado das transações, evitando guardar um total que
possa ficar desatualizado.

As respostas usam `application/problem+json` nas falhas. Mês inválido e
categoria indisponível retornam `400`, orçamento duplicado retorna `409`, e
um identificador ausente no contexto ativo retorna `404`.

## Resultado da verificação

- `mvn clean verify`: 43 testes aprovados com PostgreSQL 17, Testcontainers e
  Flyway V1 a V4; cobertura JaCoCo de 96,75% das linhas, 94,68% das
  instruções e 79,09% dos desvios nas 30 classes com regras executáveis;
- `npm run lint`, `npm run typecheck`, `npm test` e `npm run build`:
  aprovados, com 43 testes Vitest;
- `npm run test:e2e`: cenários executados em Chromium 1440 px, Firefox
  820 px e WebKit 390 px, com 36 aprovações e três cenários integrados
  ignorados nessa execução isolada;
- o fluxo REST integrado cobre transação, orçamento excedido e reflexo no
  dashboard contra a API e o PostgreSQL reais, com três aprovações em
  `npm run test:e2e:integrado`;
- a imagem `ji-back-api` foi construída pelo Compose, a migração V4 iniciou
  em PostgreSQL 17 e `/actuator/health` respondeu `UP`;
- inspeção visual do Dashboard e de Orçamentos realizada em desktop e 390 px,
  nos temas do sistema, sem erros no console.

Verificação final concluída em 06/11/2026. Nenhum commit ou push faz parte
desta entrega.
