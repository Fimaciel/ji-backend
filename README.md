# Ji Back — Sprints 1 a 6

API REST do Gerenciador Financeiro Pessoal Ji, desenvolvida com Java 21,
Spring Boot 3.5.16 e PostgreSQL 17. A Sprint 1 implementa cadastro,
autenticação, recuperação de senha e logout. A Sprint 2 acrescenta RF-02,
RF-03, RF-04 e RF-23: consulta e edição do perfil, preferências financeiras,
tema, restauração de configurações e encerramento lógico da conta.
A Sprint 3 implementa o cadastro, a listagem, a edição, a exclusão e a
categorização de entradas e saídas no contexto ativo. A Sprint 4 acrescenta
o dashboard mensal (RF-09) e os orçamentos por categoria com alertas de 80%
e de limite excedido (RF-16). A Sprint 5 implementa criação e seleção de
contextos e resposta a convites. A Sprint 6 acrescenta convites de
colaboradores, dependentes e gestão de participantes e papéis.

O acesso às rotas protegidas usa JWT HS256 com duração padrão de oito horas.
O token contém apenas `sub`, `jti`, `iat`, `exp`, `iss` e `aud`. Cada `jti`
também identifica uma sessão no PostgreSQL, o que permite invalidar o JWT no
logout ou após a redefinição da senha.

## Requisitos

- Java 21;
- Maven 3.6.3 ou superior;
- Docker com Compose, usado pelo ambiente local e pelo Testcontainers.

## Configuração

Copie o arquivo de exemplo e gere um segredo JWT com pelo menos 256 bits:

```bash
cp .env.example .env
openssl rand -base64 32
```

Substitua `JWT_SECRET_BASE64` no `.env` pelo valor gerado. O arquivo `.env`
não é versionado. As variáveis da aplicação são:

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ji` | conexão PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | `ji` / `ji` | credenciais locais |
| `JWT_SECRET_BASE64` | sem padrão | segredo HS256 obrigatório |
| `JWT_ISSUER` | `ji-back` | emissor aceito |
| `JWT_AUDIENCE` | `ji-front` | público aceito |
| `JWT_DURATION` | `PT8H` | validade do token |
| `MAIL_MODE` | `smtp` | envio real; `log` apenas para desenvolvimento |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | servidor SMTP |
| `MAIL_FROM` | `no-reply@ji.local` | remetente |
| `FRONTEND_URL` | `http://localhost:5173` | base do link de redefinição |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | origens liberadas |

As portas externas do Compose podem ser trocadas com `POSTGRES_PORT`,
`MAIL_SMTP_PORT`, `MAIL_UI_PORT` e `API_PORT`, sem alterar as portas internas
usadas entre os contêineres.

## Executar localmente

Para iniciar PostgreSQL e Mailpit e executar a aplicação pelo Maven:

```bash
docker compose up -d postgres mailpit
set -a
. ./.env
set +a
mvn spring-boot:run
```

O Mailpit mostra as mensagens em `http://localhost:8025`. Para construir e
executar também a API pelo Compose:

```bash
set -a
. ./.env
set +a
docker compose --profile app up -d --build
```

O perfil `app` usa a imagem criada pelo `Dockerfile`, publica a API na porta
`8080` e aguarda o PostgreSQL ficar saudável. Para encerrar os contêineres sem
apagar o volume do banco:

```bash
docker compose --profile app down
```

## API

| Método | Rota | Autenticação | Resposta de sucesso |
|---|---|---|---|
| `POST` | `/api/v1/auth/cadastro` | pública | `201` |
| `POST` | `/api/v1/auth/login` | pública | `200` com JWT e contextos |
| `GET` | `/api/v1/auth/sessao` | Bearer JWT | `200` |
| `POST` | `/api/v1/auth/logout` | Bearer JWT | `204` |
| `POST` | `/api/v1/auth/recuperacao-senha` | pública | `202` genérico |
| `GET` | `/api/v1/auth/redefinicao-senha/validacao?token=...` | pública | `200` com estado do token |
| `POST` | `/api/v1/auth/redefinicao-senha` | pública | `204` |
| `GET` | `/api/v1/usuarios/me` | Bearer JWT | `200` com perfil e configuração |
| `PUT` | `/api/v1/usuarios/me/perfil` | Bearer JWT | `200` com conta atualizada |
| `PUT` | `/api/v1/usuarios/me/configuracao` | Bearer JWT | `200` com configuração atualizada |
| `PATCH` | `/api/v1/usuarios/me/configuracao/tema` | Bearer JWT | `200` com tema atualizado |
| `DELETE` | `/api/v1/usuarios/me/configuracao` | Bearer JWT | `200` com padrões restaurados |
| `DELETE` | `/api/v1/usuarios/me` | Bearer JWT | `204`; conta e sessões encerradas |
| `GET` | `/api/v1/categorias` | Bearer JWT | `200` com categorias disponíveis |
| `POST` | `/api/v1/categorias` | Bearer JWT | `201` com categoria personalizada |
| `PUT` | `/api/v1/categorias/{id}` | Bearer JWT | `200` com categoria atualizada |
| `DELETE` | `/api/v1/categorias/{id}` | Bearer JWT | `204`; exclui ou arquiva a categoria |
| `GET` | `/api/v1/transacoes` | Bearer JWT | `200` com página de 20 itens |
| `POST` | `/api/v1/transacoes` | Bearer JWT | `201` com transação criada |
| `PUT` | `/api/v1/transacoes/{id}` | Bearer JWT | `200` com transação atualizada |
| `DELETE` | `/api/v1/transacoes/{id}` | Bearer JWT | `204`; exclusão auditada |
| `GET` | `/api/v1/dashboard?mes=AAAA-MM` | Bearer JWT | `200` com resumo mensal, fluxo, categorias e lançamentos recentes |
| `GET` | `/api/v1/orcamentos?mes=AAAA-MM` | Bearer JWT | `200` com limites e consumo do mês |
| `POST` | `/api/v1/orcamentos` | Bearer JWT | `201` com orçamento criado |
| `PUT` | `/api/v1/orcamentos/{id}` | Bearer JWT | `200` com orçamento atualizado |
| `DELETE` | `/api/v1/orcamentos/{id}` | Bearer JWT | `204`; exclusão auditada |
| `GET` | `/api/v1/contextos` | Bearer JWT | `200` com contextos e indicação do ativo |
| `POST` | `/api/v1/contextos` | Bearer JWT | `201` com contexto compartilhado |
| `PUT` | `/api/v1/contextos/ativo` | Bearer JWT | `200` com contexto selecionado |
| `GET` | `/api/v1/contextos/ativo/convites` | Bearer JWT | `200` com convites enviados |
| `POST` | `/api/v1/contextos/ativo/convites` | Bearer JWT | `201` e envio do convite |
| `POST` | `/api/v1/contextos/ativo/convites/{id}/reenvio` | Bearer JWT | `200` com nova validade |
| `DELETE` | `/api/v1/contextos/ativo/convites/{id}` | Bearer JWT | `204`; cancela convite pendente |
| `GET` | `/api/v1/contextos/ativo/participantes` | Bearer JWT | `200` com ativos e histórico |
| `PATCH` | `/api/v1/contextos/ativo/participantes/{id}/papel` | Bearer JWT | `200` com papel atualizado |
| `DELETE` | `/api/v1/contextos/ativo/participantes/{id}` | Bearer JWT | `204`; encerra participação |
| `DELETE` | `/api/v1/contextos/ativo/participantes/me` | Bearer JWT | `204`; saída voluntária |
| `GET` | `/api/v1/dependentes` | Bearer JWT | `200` com dependentes ativos |
| `POST` | `/api/v1/dependentes` | Bearer JWT | `201`; pode enviar convite de leitura |
| `PUT` | `/api/v1/dependentes/{id}` | Bearer JWT | `200` com dependente atualizado |
| `DELETE` | `/api/v1/dependentes/{id}` | Bearer JWT | `204`; exclusão lógica e revogação do acesso |
| `GET` | `/actuator/health` | pública | `200` |

Cadastro:

```bash
curl -i http://localhost:8080/api/v1/auth/cadastro \
  -H 'Content-Type: application/json' \
  -d '{
    "nome": "Filipe",
    "sobrenome": "Lopes",
    "email": "filipe@example.com",
    "senha": "senha-segura"
  }'
```

Login e consulta da sessão:

```bash
curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"filipe@example.com","senha":"senha-segura"}'

curl -i http://localhost:8080/api/v1/auth/sessao \
  -H "Authorization: Bearer SEU_TOKEN"
```

Recuperação e redefinição:

```bash
curl -i http://localhost:8080/api/v1/auth/recuperacao-senha \
  -H 'Content-Type: application/json' \
  -d '{"email":"filipe@example.com"}'

curl -i http://localhost:8080/api/v1/auth/redefinicao-senha \
  -H 'Content-Type: application/json' \
  -d '{
    "token": "TOKEN_RECEBIDO_NO_EMAIL",
    "novaSenha": "nova-senha-segura",
    "confirmacaoSenha": "nova-senha-segura"
  }'
```

Perfil e configuração, usando o JWT retornado no login:

```bash
curl -s http://localhost:8080/api/v1/usuarios/me \
  -H "Authorization: Bearer SEU_TOKEN"

curl -i -X PUT http://localhost:8080/api/v1/usuarios/me/configuracao \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"salario":"7250.50","moeda":"BRL","notificacoesAtivas":true}'

curl -i -X PATCH http://localhost:8080/api/v1/usuarios/me/configuracao/tema \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"tema":"ESCURO"}'
```

CPF e identificador não fazem parte do contrato de atualização. Alterações de
perfil exigem a senha atual; a nova senha é opcional e deve vir acompanhada de
confirmação. O encerramento é lógico: os registros são preservados,
`data_exclusao` é preenchida e sessões e tokens de redefinição são revogados.

As falhas são retornadas em `application/problem+json`, com mensagem em
português e um campo `codigo` estável. Credenciais ou JWT inválidos retornam
`401`; dados inválidos, `400`; conflitos de e-mail ou CPF, `409`; falha do
SMTP durante um envio real, `503`.

Categorias e transações, usando o JWT retornado no login:

```bash
curl -s http://localhost:8080/api/v1/categorias \
  -H "Authorization: Bearer SEU_TOKEN"

curl -i http://localhost:8080/api/v1/transacoes \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "tipo": "SAIDA",
    "valor": 184.50,
    "data": "2026-11-03",
    "categoriaId": 1,
    "descricao": "Supermercado"
  }'

curl -s 'http://localhost:8080/api/v1/transacoes?tipo=SAIDA&pagina=0' \
  -H "Authorization: Bearer SEU_TOKEN"
```

As cinco categorias padrão são Alimentação, Transporte, Saúde, Lazer e
Salário. Categorias personalizadas pertencem ao contexto ativo. Ao excluir uma
categoria já usada, ela deixa de aparecer em novos lançamentos, mas permanece
associada ao histórico.

Dashboard e orçamento, usando o JWT retornado no login:

```bash
curl -s 'http://localhost:8080/api/v1/dashboard?mes=2026-11' \
  -H "Authorization: Bearer SEU_TOKEN"

curl -i http://localhost:8080/api/v1/orcamentos \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "categoriaId": 1,
    "valorLimite": 800.00,
    "mesReferencia": "2026-11"
  }'

curl -s 'http://localhost:8080/api/v1/orcamentos?mes=2026-11' \
  -H "Authorization: Bearer SEU_TOKEN"
```

O dashboard é calculado a partir das transações confirmadas do contexto ativo.
Nos orçamentos, somente saídas do mês e da categoria consomem o limite. O
estado passa a `ATENCAO` a partir de 80% e a `EXCEDIDO` a partir de 100%.
Existe no máximo um orçamento por categoria, contexto e mês.

Contextos e convites, usando o JWT retornado no login:

```bash
curl -s http://localhost:8080/api/v1/contextos \
  -H "Authorization: Bearer SEU_TOKEN"

curl -i http://localhost:8080/api/v1/contextos \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Família"}'

curl -i -X PUT http://localhost:8080/api/v1/contextos/ativo \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"contextoId":2}'

curl -s http://localhost:8080/api/v1/convites \
  -H "Authorization: Bearer SEU_TOKEN"
```

Quando a conta possui mais de um contexto, o login não escolhe um deles por
suposição: retorna `selecaoContextoNecessaria=true` até a chamada de seleção.
Convites são consultados pelo destinatário e só podem ser respondidos uma vez.

Colaboração no contexto compartilhado:

```bash
curl -i http://localhost:8080/api/v1/contextos/ativo/convites \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"email":"colaborador@example.com"}'

curl -i http://localhost:8080/api/v1/dependentes \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "nome":"Pessoa dependente",
    "dataNascimento":"2012-08-21",
    "tipo":"LEITURA",
    "email":"dependente@example.com"
  }'

curl -s http://localhost:8080/api/v1/contextos/ativo/participantes \
  -H "Authorization: Bearer SEU_TOKEN"
```

Somente administradores de contextos compartilhados enviam convites,
gerenciam dependentes e alteram participantes. O contexto nunca pode ficar
sem administrador. O papel `DEPENDENTE` é criado apenas pelo convite associado
a um cadastro do tipo `LEITURA`.

## Testes e cobertura

```bash
mvn clean verify
```

O comando inicia um PostgreSQL isolado com Testcontainers, executa as
migrações Flyway V1 a V6, roda os testes e exige ao menos 60% de cobertura de
linhas das classes com regras executáveis. O relatório HTML fica em
`target/site/jacoco/index.html`.

Para validar apenas a imagem:

```bash
docker build -t ji-back:sprint-6 .
```

A rastreabilidade da Sprint 1 está em
[`docs/sprint-1-rastreabilidade.md`](docs/sprint-1-rastreabilidade.md). A
matriz consolidada da Sprint 2 está em
[`docs/sprint-2-rastreabilidade.md`](docs/sprint-2-rastreabilidade.md). A
Sprint 3 está registrada em
[`docs/sprint-3-rastreabilidade.md`](docs/sprint-3-rastreabilidade.md).
A Sprint 4 está em
[`docs/sprint-4-rastreabilidade.md`](docs/sprint-4-rastreabilidade.md).
A Sprint 5 está em
[`docs/sprint-5-rastreabilidade.md`](docs/sprint-5-rastreabilidade.md).
A Sprint 6 está em
[`docs/sprint-6-rastreabilidade.md`](docs/sprint-6-rastreabilidade.md).

Em produção, a API deve ficar atrás de HTTPS, usar segredo JWT e credenciais
SMTP/PostgreSQL próprios e restringir `CORS_ALLOWED_ORIGINS` ao domínio real
do front-end.
