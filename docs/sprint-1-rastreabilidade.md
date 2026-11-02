# Rastreabilidade da Sprint 1

Período planejado no Projeto de Software: 01/09/2026 a 14/09/2026. Execução
real do back-end: 02/10/2026 a 06/10/2026. O escopo desta matriz é o
`ji-back`; comportamentos exclusivos da interface não são atribuídos à API.

## Requisitos funcionais e regras associadas

| Requisito | Evidência de implementação | Evidência automatizada | Situação |
|---|---|---|---|
| RF-01 — Cadastrar usuário | `POST /api/v1/auth/cadastro`; `AutenticacaoService.cadastrar` | `AutenticacaoServiceTest.cadastroNormalizaDadosECriaEstruturasIniciais`; `AuthApiIntegrationTest.cadastroCriaContaConfiguracaoContextoEParticipacaoNaMesmaOperacao` | Coberto |
| NF1.1 — campos, opcionais e e-mail ativo único | validação dos DTOs; normalização para minúsculas; índice parcial `usuario_email_ativo_uk`; CPF e nascimento opcionais | testes de cadastro, duplicidade e validação em `AuthApiIntegrationTest` | Coberto |
| NF1.2 — bcrypt com salt | `BCryptPasswordEncoder(12)`; somente o hash é persistido | cadastro integrado e troca de senha; `AutenticacaoServiceTest` | Coberto |
| NF1.3 — validação obrigatória | Bean Validation para nome, sobrenome, e-mail, CPF, data e senha | `duplicidadesEValidacoesRetornamProblemJsonSemCriarEstruturasParciais`; `SenhaValidatorTest` | Coberto |
| NF1.4 — mensagens | respostas HTTP e `application/problem+json` com códigos estáveis | testes de validação, conflito e JSON malformado | Coberto no back-end |
| NF1.5 — datas do usuário | `data_criacao` e `data_atualizacao` na migração e entidade | cadastro e migração real em `AuthApiIntegrationTest` | Coberto |
| RF-10 — Login | `POST /api/v1/auth/login`; JWT HS256; tabela `sessao`; `GET /api/v1/auth/sessao` | `loginAtivaAutomaticamenteOUnicoContextoERegistraAuditoria`; `loginComMaisDeUmContextoSinalizaSelecaoSemAtivarUmDeles`; fluxo integrado de JWT/sessão | Coberto |
| NF10.1 — comparação com bcrypt e transporte protegido | comparação por `PasswordEncoder.matches`; nenhuma senha é gravada em token ou log | testes de credenciais e claims JWT | Coberto na aplicação; HTTPS depende da implantação |
| NF10.2 — rotas protegidas | Spring Security Resource Server, validação de assinatura, emissor, público, expiração e sessão por `jti` | `JwtConfigTest`; `SessaoServiceTest`; `credenciaisInvalidasEOuAusenciaDeJwtRetornam401` | Coberto |
| RF-13 — Recuperar senha | `POST /api/v1/auth/recuperacao-senha` e `/redefinicao-senha`; SMTP; SHA-256; uso único; revogação de sessões | `RecuperacaoSenhaServiceTest`; `SmtpPasswordResetMailSenderTest`; fluxo integrado de recuperação | Coberto |
| NF13.1 — link por e-mail | `PasswordResetMailSender` e implementação SMTP | teste unitário do SMTP e capturador controlado no teste integrado | Coberto |
| NF13.2 — expiração em 30 minutos | `TokenRedefinicaoSenha.dataExpiracao`; verificação de validade | testes unitários de expiração e teste integrado da duração | Coberto |
| RF-15 — Encerrar sessão | `POST /api/v1/auth/logout`; `sessao.data_revogacao` | `logoutRevogaSessaoEAuditaESessaoAtualRetornaValidade`; fluxo integrado de logout | Coberto |
| NF15.1 — opção acessível nas telas | responsabilidade do `ji-front` | não se aplica ao back-end | Fora desta execução |
| NF15.2 — invalidação imediata | consulta da sessão por `jti` em toda requisição; preenchimento de `data_revogacao` | JWT aceito antes do logout e rejeitado depois no teste integrado | Coberto |
| NF15.3 — redirecionamento ao login | responsabilidade do `ji-front`; a API responde `204` | não se aplica ao back-end | Fora desta execução |
| NF15.4 — data/hora de encerramento | `sessao.data_revogacao` e log `LOGOUT` | testes unitário e integrado de logout/auditoria | Coberto |
| RF-17, apenas criação inicial | cadastro transacional cria configuração, contexto `PESSOAL` e participação `ADMINISTRADOR` | testes unitário e integrado do cadastro | Coberto no recorte da Sprint |
| NF17.1 | contexto Pessoal criado junto com a conta | teste integrado do cadastro | Coberto |
| NF17.3 | índice parcial `contexto_pessoal_usuario_uk` | Flyway executado em PostgreSQL vazio | Coberto para o contexto Pessoal; contextos compartilhados ficam para Sprint futura |
| NF17.4 | participação inicial `ADMINISTRADOR` | testes unitário e integrado do cadastro | Coberto |
| NF17.5 | datas de criação e atualização do contexto | colunas obrigatórias na migração e entidade | validação JPA/Flyway e cadastro integrado | Coberto |
| NF17.2 | criação manual de contexto Compartilhado | não pertence ao recorte aprovado da Sprint 1 | Sprint futura |

O cadastro inteiro é executado por um único método `@Transactional`. Os
índices do banco também impedem e-mail ativo duplicado, CPF duplicado, mais de
um contexto Pessoal e mais de uma participação ativa para o mesmo par
usuário/contexto. Em caso de erro, a transação não deixa configuração,
contexto ou participação parciais.

## Requisitos não funcionais gerais aplicáveis

| Requisito | Evidência | Situação na Sprint 1 |
|---|---|---|
| RNF-01 — bcrypt e isolamento por contexto | bcrypt; JWT assinado; sessão revogável; validação de participação ativa quando há contexto na sessão | Coberto no back-end; HTTPS é requisito de implantação |
| RNF-02 — minimização/LGPD | CPF e nascimento opcionais; JWT e logs não recebem CPF nem senha | Coberto no recorte de cadastro/autenticação |
| RNF-08 — organização em camadas | pacotes `controller`, `service`, `repository`, `model`, `security` e `config` | Coberto |
| RNF-09 — auditoria | logs de login, solicitação/conclusão da redefinição e logout com usuário e data/hora | Coberto para as ações da Sprint 1 |
| RNF-10 — pt-BR | validações e problemas em português; configuração padrão `BRL` | Coberto na API da Sprint; máscara e exibição de datas pertencem ao front-end |
| RNF-11 — cobertura mínima de 60% | regra JaCoCo no `mvn verify` | Coberto: 96,82% de linhas em 06/10/2026 |

RNF-03 a RNF-07 tratam desempenho de listagens/relatórios, disponibilidade da
instância e compatibilidade da interface. Eles não possuem critério executável
neste incremento de autenticação e serão verificados nas Sprints
correspondentes ou na implantação.

## Resultado da verificação

- `mvn clean verify`: aprovado em 06/10/2026;
- 32 testes, sem falhas ou erros;
- cobertura JaCoCo: 96,82% de linhas, 96,97% de instruções e 87,50% de ramos;
- Flyway V1 aplicado em PostgreSQL 17 vazio pelo Testcontainers;
- H2 não faz parte das dependências nem dos testes;
- imagem `ji-back:sprint-1` construída com sucesso;
- smoke test aprovado com a API em contêiner, PostgreSQL e Mailpit: health,
  cadastro, login, sessão, recuperação por SMTP, redefinição, revogação das
  sessões antigas e logout;
- relatório de cobertura: `target/site/jacoco/index.html` após o build.
