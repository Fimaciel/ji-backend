# Ji Back — Sprints 1 e 2

API REST do Gerenciador Financeiro Pessoal Ji, desenvolvida com Java 21,
Spring Boot 3.5.16 e PostgreSQL 17. A Sprint 1 implementa cadastro,
autenticação, recuperação de senha e logout. A Sprint 2 acrescenta RF-02,
RF-03, RF-04 e RF-23: consulta e edição do perfil, preferências financeiras,
tema, restauração de configurações e encerramento lógico da conta.

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

## Testes e cobertura

```bash
mvn clean verify
```

O comando inicia um PostgreSQL isolado com Testcontainers, executa as
migrações Flyway V1 e V2, roda os testes e exige ao menos 60% de cobertura de
linhas das classes com regras executáveis. O relatório HTML fica em
`target/site/jacoco/index.html`.

Para validar apenas a imagem:

```bash
docker build -t ji-back:sprint-2 .
```

A rastreabilidade da Sprint 1 está em
[`docs/sprint-1-rastreabilidade.md`](docs/sprint-1-rastreabilidade.md). A
matriz consolidada da Sprint 2 está em
[`docs/sprint-2-rastreabilidade.md`](docs/sprint-2-rastreabilidade.md).

Em produção, a API deve ficar atrás de HTTPS, usar segredo JWT e credenciais
SMTP/PostgreSQL próprios e restringir `CORS_ALLOWED_ORIGINS` ao domínio real
do front-end.
