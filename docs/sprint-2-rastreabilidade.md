# Rastreabilidade da Sprint 2

Fonte funcional: Projeto de Software do TCC1, em `contextos/software.pdf`.
Escopo implementado: RF-02, RF-03, RF-04 e RF-23 no `ji-back` e no
`ji-front`. Esta matriz não altera nem complementa a documentação do TCC2.

## Requisitos e regras de negócio

| Requisito | Evidência no back-end | Evidência no front-end | Verificação automatizada | Situação |
|---|---|---|---|---|
| RF-02 — Editar perfil | `GET /api/v1/usuarios/me`; `PUT /api/v1/usuarios/me/perfil`; `ContaUsuarioService.atualizarPerfil` | seção “Dados pessoais” e “Segurança” em `/app/configuracoes` | `ContaUsuarioApiIntegrationTest.consultaEAtualizaPerfilSemPermitirCpfOuIdentificador`; `fluxosConfiguracoes.test.tsx` | Coberto |
| NF2.1 — nome, sobrenome, e-mail, nascimento e senha opcional | DTO `AtualizacaoPerfilRequest`; normalização do e-mail; bcrypt para nova senha | React Hook Form e Zod com mensagens por campo | testes integrados de atualização e bcrypt; testes de formulário e sessão | Coberto |
| NF2.2 — CPF e identificador imutáveis | o contrato de escrita não expõe esses campos | CPF desabilitado e identificado como somente leitura | teste integrado confirma CPF e ID preservados | Coberto |
| NF2.3 — senha atual e confirmação da nova senha | `PasswordEncoder.matches`; validação cruzada no DTO | senha atual obrigatória; confirmação condicional | testes de senha inválida/divergente e de formulário | Coberto |
| NF2.4 — e-mail único entre contas ativas | índice parcial da V1 e consulta `existsByEmailIgnoreCaseAndDataExclusaoIsNullAndIdNot` | erro da API associado ao campo de e-mail | teste integrado de e-mail ativo duplicado | Coberto |
| NF2.5 — atualização temporal e auditoria | `Usuario.registrarAtualizacao`; evento `ATUALIZACAO_PERFIL` | sessão exibida é atualizada após sucesso | testes de integração e de atualização da sessão | Coberto |
| RF-03 — Configurar salário/moeda/notificações | `PUT /api/v1/usuarios/me/configuracao`; `ContaUsuarioService.atualizarConfiguracao` | seção “Preferências financeiras” | teste integrado de configuração; testes REST, mock, formulário e Playwright | Coberto |
| NF3.1 — salário não negativo com duas casas | Bean Validation e `numeric(15,2)`; `CHECK (salario >= 0)` na V2 | máscara monetária pt-BR e validação Zod | Flyway real no Testcontainers e testes de contrato/formulário | Coberto |
| NF3.2 — moeda BRL | enum `Moeda.BRL` e `CHECK (moeda = 'BRL')` na V2 | BRL bloqueado para edição | teste integrado e teste do adaptador REST | Coberto |
| NF3.3 — notificações | valor booleano persistido em `ConfiguracaoUsuario` | controle acessível para ativar/desativar | testes integrado, mock e de formulário | Coberto |
| RF-04 — Restaurar configurações | `DELETE /api/v1/usuarios/me/configuracao`; evento `RESTAURACAO_CONFIGURACAO` | modal de confirmação de restauração | `configuraTemaERestauraPadroesComAuditoria`; Vitest e Playwright | Coberto |
| NF4.1 — padrões | salário `0,00`, moeda `BRL`, notificações ativas e tema `AUTOMATICO` | interface reconcilia os valores retornados | teste integrado e teste de fluxo do front | Coberto |
| NF4.2 — confirmação | operação explícita e auditada | modal acessível antes da chamada REST | Vitest e Playwright | Coberto |
| NF4.4 — cancelamento de convites pendentes | nenhuma tabela futura foi antecipada | não aplicável neste incremento | registrado como dependência de `CONVITE_CONTEXTO` | Cobertura parcial até a Sprint de convites |
| Tema | `PATCH /api/v1/usuarios/me/configuracao/tema`; enum `CLARO`, `ESCURO`, `AUTOMATICO`; evento `ALTERACAO_TEMA` | aplicação imediata em `data-tema`, cache local e `matchMedia` em tempo real | teste integrado; `fluxosConfiguracoes.test.tsx`; Playwright | Coberto |
| RF-23 — Encerrar conta | `DELETE /api/v1/usuarios/me`; `Usuario.encerrar`; revogação de sessões e tokens | zona de perigo; modal exige `EXCLUIR`; armazenamento Ji é limpo | `encerramentoLogicoRevogaAcessosEPreservaRegistros`; Vitest e Playwright | Coberto |
| NF23 — preservação e bloqueio de acesso | `data_exclusao`; dados preservados; login filtra conta ativa; tokens e sessões revogados; sem reativação | texto informa preservação e redireciona ao login | teste integrado verifica registros preservados e JWT/login bloqueados | Coberto |
| Validação REST da recuperação | `GET /api/v1/auth/redefinicao-senha/validacao` | adaptador REST consulta o estado antes da redefinição | `AuthApiIntegrationTest` e `ServicoRest.test.ts` | Coberto |

## Requisitos não funcionais aplicáveis

| Requisito | Evidência | Situação |
|---|---|---|
| Segurança e LGPD | bcrypt, JWT revogável, CPF não editável, encerramento lógico, ausência de senha/CPF no armazenamento do navegador | Coberto no incremento; HTTPS permanece responsabilidade da implantação |
| Auditoria | eventos de atualização de perfil/configuração/tema, restauração e encerramento com data e usuário | Coberto |
| RNF-08 — organização | back em camadas; front separado em serviços, contexto, tema, validações, componentes e telas | Coberto |
| RNF-10 — pt-BR | textos, mensagens por campo e máscara monetária pt-BR; moeda BRL | Coberto |
| Acessibilidade | foco visível, rótulos, regiões de estado, teclado, `dialog` com foco contido e Escape, contraste claro/escuro | Coberto por testes de componente e Playwright; validação manual recomendada em leitor de tela real |
| Responsividade/compatibilidade | layout interno nos viewports 390, 820 e 1440 px | Coberto em WebKit, Firefox e Chromium |
| RNF-11 — cobertura mínima do back | regra JaCoCo de 60% no `mvn verify` | Coberto: 96,32% de linhas na execução de 02/11/2026 |

## Resultado da verificação

- `mvn clean verify`: aprovado; 36 testes, sem falhas; Flyway V1 e V2 em
  PostgreSQL 17 via Testcontainers; cobertura JaCoCo de 96,32% de linhas,
  95,03% de instruções e 79,03% de ramos;
- `npm run lint`: aprovado;
- `npm run typecheck`: aprovado;
- `npm test`: aprovado; 33 testes Vitest;
- `npm run test:e2e`: aprovado nos três navegadores/viewports com o mock
  explicitamente ativado;
- `npm run test:e2e:integrado`: aprovado em Chromium, Firefox e WebKit contra
  a API REST, PostgreSQL e Mailpit reais;
- `npm run build`: aprovado.

Os relatórios locais ficam em `ji-back/target/site/jacoco/index.html` e
`ji-front/playwright-report/index.html`. Nenhum commit ou push faz parte desta
entrega.
