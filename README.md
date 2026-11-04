# commerce-bff-nani

BFF (*backend for frontend*) da **Nani Perfumes**: a única porta do site (`commerce-web`) para o back.

- Faz o login no Keycloak (`commerce-auth`, realm `nani`) com código de autorização + PKCE.
- **Guarda os tokens no servidor, só na memória** (nunca em disco nem em backup). O navegador recebe só o cookie `NANI_SESSION` (HttpOnly) e o `XSRF-TOKEN` (proteção CSRF). Reiniciar o BFF encerra as sessões (deploy fora do horário comercial); o Keycloak religa o cliente sem pedir senha. Mais de uma cópia do BFF → Redis (ver pendência A1).
- Uma rota por necessidade do site, chamando a `commerce-api` com o token da sessão. Sem regra de negócio.
- Swagger: `http://localhost:8090/swagger-ui.html` (com o BFF rodando) e `docs/swagger.yaml`, gerado a cada `./mvnw install`/`verify` — rodar sempre que mudar alguma rota ou DTO.

## Rodar na sua máquina

Pré-requisitos: Docker Desktop aberto e o `commerce-auth` no ar (`docker compose up -d` lá, uma vez — ele fica ligado).

```bash
cp .env.example .env      # mesmo segredo do COMMERCE_BFF_CLIENT_SECRET do commerce-auth
./mvnw spring-boot:run    # sobe o BFF na porta 8090
```

O site acessa o BFF pelo proxy do `ng serve` (tudo em `http://localhost:4200`, como em produção).

| Rota | O que faz |
|---|---|
| `GET /bff/auth/login?returnTo=/caminho` | Entrar: vai para a tela de login da Nani e volta para `/caminho` |
| `GET /bff/auth/register?returnTo=/caminho` | Criar conta: abre direto o cadastro |
| `POST /bff/auth/logout` (com cabeçalho CSRF) | Sair: apaga a sessão e devolve `{ "redirectUrl": ... }` para sair também do Keycloak |
| `GET /bff/auth/session` | Quem está logado: `{authenticated, user}`, sempre 200 |
| `GET /actuator/health` | Saúde |

## Testes

```bash
./mvnw test    # não precisa de Docker nem do Keycloak
```

## Configuração

`src/main/resources/application.properties`, prefixos `nani.auth` e `nani.api`. Em produção, por variável de ambiente: `COMMERCE_API_URL`, `KEYCLOAK_PUBLIC_URL`, `KEYCLOAK_INTERNAL_URL`, `NANI_AUTH_CLIENT_SECRET`, `NANI_AUTH_REDIRECT_URI`, `NANI_AUTH_POST_LOGOUT_URI`, `COOKIE_SECURE=true`.
