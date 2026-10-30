# commerce-bff-nani

BFF (*backend for frontend*) da **Nani Perfumes**: a única porta do site (`commerce-web`) para o back.

- Faz o login no Keycloak (`commerce-auth`, realm `nani`) com código de autorização + PKCE.
- **Guarda os tokens no servidor, só na memória** (nunca em disco nem em backup). O navegador recebe só o cookie `NANI_SESSION` (HttpOnly) e o `XSRF-TOKEN` (proteção CSRF). Reiniciar o BFF encerra as sessões (deploy fora do horário comercial); o Keycloak religa o cliente sem pedir senha. Mais de uma cópia do BFF → Redis (ver pendência A1).
- Repassa as chamadas do site para a `commerce-api` (a partir da B3). Sem regra de negócio.

## Rodar na sua máquina

Pré-requisitos: Docker Desktop aberto e o `commerce-auth` no ar (`docker compose up -d` lá, uma vez — ele fica ligado).

```bash
cp .env.example .env      # mesmo segredo do COMMERCE_BFF_CLIENT_SECRET do commerce-auth
./mvnw spring-boot:run    # sobe o BFF na porta 8090
```

O site acessa o BFF pelo proxy do `ng serve` (tudo em `http://localhost:4200`, como em produção).

| Rota | O que faz |
|---|---|
| `GET /api/auth/login?returnTo=/caminho` | Entrar: vai para a tela de login da Nani e volta para `/caminho` |
| `GET /api/auth/register?returnTo=/caminho` | Criar conta: abre direto o cadastro |
| `POST /api/auth/logout` (com cabeçalho CSRF) | Sair: apaga a sessão e devolve `{ "redirectUrl": ... }` para sair também do Keycloak |
| `GET /api/me` | Quem está logado (nome, e-mail, papéis, permissões) ou 401 |
| `GET /actuator/health` | Saúde |

## Testes

```bash
./mvnw test    # não precisa de Docker nem do Keycloak
```

## Configuração

`src/main/resources/application.yml`, prefixo `nani.auth`. Em produção, por variável de ambiente: `KEYCLOAK_PUBLIC_URL`, `KEYCLOAK_INTERNAL_URL`, `NANI_AUTH_CLIENT_SECRET`, `NANI_AUTH_REDIRECT_URI`, `NANI_AUTH_POST_LOGOUT_URI`, `COOKIE_SECURE=true`.
