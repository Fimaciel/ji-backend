# Rastreabilidade da entrega final

Fonte funcional: Projeto de Software do TCC I, em `contextos/software.pdf`.
Período planejado: 24/11/2026 a 07/12/2026, com 16 horas. Execução real
antecipada: 09/11/2026. Esta entrega implementa RF-05 e RF-08 no back-end e
no front-end. O período de 08/12/2026 a 19/12/2026 foi tratado como
estabilização: revisão de regressões, build e validação do ambiente.

## Requisitos e evidências

| Requisito | API e implementação | Interface | Evidência automatizada | Situação |
|---|---|---|---|---|
| RF-05 — Importar entrada/saída | análise multipart em `POST /api/v1/importacoes/analise`; confirmação em `POST /api/v1/importacoes` | fluxo em `/app/importacoes`, com seleção, revisão e confirmação | `ImportacoesRelatoriosApiIntegrationTest.analisaConfirmaIgnoraDuplicadosListaEDesfazImportacaoXlsx`; testes Vitest e Playwright | Coberto |
| PDF com texto selecionável | Apache PDFBox extrai linhas; PDF sem texto recebe `pdf-sem-texto` | mensagem da API é exibida no painel | `aceitaPdfComTextoERejeitaPdfDigitalizado` | Coberto |
| XLSX padronizado | Apache POI lê a primeira aba e exige data, descrição, valor e tipo | orientações de formato e limite junto ao seletor | teste integrado cria e processa uma planilha real | Coberto |
| Revisão antes da persistência | análise não grava transações; confirmação recebe apenas registros selecionados e categorias revisadas | duplicados ficam desabilitados e categorias podem ser ajustadas | contagem do repositório antes/depois e fluxo de componente | Coberto |
| Duplicidade | comparação por contexto, data, valor e descrição; duplicados do arquivo também são detectados | totais lidos, novos e duplicados | teste com lançamento previamente existente | Coberto |
| Desfazer última importação | `DELETE /api/v1/importacoes/ultima`; exclusão transacional dos lançamentos vinculados | confirmação e histórico com estado `DESFEITA` | teste integrado e fluxo do front-end | Coberto |
| Auditoria | eventos `IMPORTACAO_TRANSACOES` e `DESFAZIMENTO_IMPORTACAO`, com arquivo e contagens | histórico mostra arquivo, data, quantidade e estado | asserções em `LogAuditoriaRepository` | Coberto |
| RF-08 — Relatório geral | `GET /api/v1/relatorios/resumo` com período, tipo, categoria e contextos | filtros e resumo em `/app/relatorios` | `resumeEExportaRelatorioEmPdfEXlsx`; testes mock, REST e componente | Coberto |
| Consolidação de contextos | somente participações ativas e acessíveis ao usuário são aceitas | seletor aparece para contas com mais de um contexto | serviço valida todos os IDs antes da consulta | Coberto |
| Exportação PDF/XLSX | `GET /api/v1/relatorios/exportacao`; geração sob demanda por PDFBox e POI | botões de download com nome retornado no cabeçalho | leitura do `%PDF` e abertura da planilha pelo POI no teste | Coberto |
| Relatório sem dados | resumo zerado; exportação retorna `422 relatorio-sem-dados` | orientação de ausência e exportações desabilitadas | `naoExportaRelatorioSemDados` | Coberto |
| Permissões e isolamento | leitura exige participação ativa; importação exige administrador ou colaborador | dependente visualiza relatório e não recebe controles de escrita | serviços reutilizam `ContextoAtivoService` e testes autenticados | Coberto |

## Banco e contrato

A migração V7 cria `importacao_arquivo`, relaciona cada transação importada ao
arquivo de origem e adiciona a categoria padrão `Outros`. O desfazimento
preserva o registro técnico da importação e marca `DESFEITA`, mas remove os
lançamentos associados. Arquivos não são persistidos. O limite é de 10 MB e
1.000 movimentações por análise.

O relatório é calculado sob demanda. As exportações não gravam arquivos no
servidor. Os problemas seguem `application/problem+json`; arquivo grande
retorna `413`, conteúdo ou formato inválido retorna `400` e exportação vazia
retorna `422`.

## Verificação concluída

- `mvn clean verify`: 54 testes aprovados, Flyway V1 a V7 em PostgreSQL 17 e
  cobertura JaCoCo de 91,85% das linhas, 89,55% das instruções e 64,66% dos
  desvios nas 56 classes avaliadas;
- `npm test`: 52 testes Vitest aprovados, incluindo mock, adaptador REST e os
  fluxos das telas finais;
- `npm run lint`, `npm run typecheck` e `npm run build`: aprovados;
- `npm run test:e2e`: 45 cenários aprovados em Chromium (1440 px), Firefox
  (820 px) e WebKit (390 px); três cenários REST foram ignorados nessa suíte;
- imagem `ji-back:latest` construída; Compose aplicou a V7 sobre o banco local,
  a API respondeu `UP` e as telas foram inspecionadas contra a API real.

Nenhum commit ou push faz parte desta execução.
