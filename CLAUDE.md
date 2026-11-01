# CLAUDE.md

Orienta o Claude ao trabalhar neste repositório.

## O que é

`commerce-bff-nani`: BFF da Nani Perfumes (Java 21, Spring Boot 4.1). Arquitetura completa no projeto: `claude/roadmap-mvp.md` ("Arquitetura" e "Fluxo entre serviços") e `claude/contrato-api.md`.

- **Fala só com o Keycloak (login) e com a `commerce-api`.** Nunca com a `commerce-payments`.
- **Sem regra de negócio.** Repassa e monta respostas para as telas; quem decide é a `commerce-api`.
- **Token nunca vai para o navegador.** Sessão **em memória** (decisão A1: tokens nunca em disco; Redis só quando houver mais de uma cópia); **o BFF não tem banco**; cookie `NANI_SESSION` HttpOnly; CSRF por cookie `XSRF-TOKEN` + cabeçalho `X-XSRF-TOKEN`.
- `/api/auth/**` é do BFF (entrar, criar conta, sair, sessão). **Cada necessidade do front é uma rota explícita** (`controller/` → `service/` → `client/`), que chama a `commerce-api` com o access token da sessão (`ApiAccessToken`: renovado sozinho, um por sessão de cada vez). **Nada de repasse genérico**: rota não declarada nunca chega à API (decisão de 31/10, defesa em camadas). Sem sessão em rota que exige login → 401; nunca redireciona (o Angular decide).
- **Erros:** regra e validação vêm da `commerce-api` e **passam pelo BFF como vieram** (status, `code`, `detail`). O BFF só tem os erros dele: rota inexistente 404, sem sessão 401, CSRF 403, parâmetro malformado 400, API fora do ar ou lenta 502 (`code: SERVICE_UNAVAILABLE`), todos em `ProblemDetail` com `code` + `detail` em português.
- **Swagger** em `http://localhost:8090/swagger-ui.html` (`SwaggerConfig`) e em `docs/swagger.yaml`, **regenerado pelo `./mvnw install`/`verify`** (sobe o BFF na porta 8095, como a `commerce-api` faz na 8085). Controller enxuto, **mesmo padrão da `commerce-api`**: `@Tag(name = "Área - Assunto", description = "frase curta")` na classe (`Público`, `Área do Cliente`, `Administração`) e `@Operation(summary = "Verbo + objeto")` de uma linha por rota (detalhe vai no Javadoc). Respostas comuns (401, 502, 302 do login) ficam num lugar só, no `SwaggerConfig`. Commitar o `swagger.yaml` junto. Em produção, `SWAGGER_ENABLED=false`.
- Tokens guardados **na sessão** (`HttpSessionOAuth2AuthorizedClientRepository`): somem no logout.
- Formatos e nomes seguem o front (`commerce-web`): ele manda.

## Convenções

Pacote base `br.com.george.commerce.bff`, **por camada, como o `commerce-api`**:

- `config/`: segurança, cliente OIDC, propriedades
- `security/`: peças do login (PKCE/cadastro, para onde voltar, logout, CSRF)
- `controller/`: só recebe e delega; nomes e pacotes em português, por público (`publico/`, `me/`, `admin/`)
- `client/`: clientes HTTP para a `commerce-api`, um por recurso (prazos curtos, token da sessão, erros da API repassados como vieram)
- `service/`: interfaces; implementações em `service/impl/` (como na `commerce-api`). Junta e repassa o que vem do `client` (sem regra de negócio)
- `dto/`: **todos** os records: os do front (`PerfilResponse`) e os do contrato da `commerce-api` (sufixo `ApiResponse`, ex.: `UsuarioApiResponse`). `client/` só tem as chamadas HTTP, como **HTTP Service Clients** (interface com `@HttpExchange`/`@GetExchange`…, o substituto do Feign recomendado pelo próprio Spring Cloud OpenFeign), registradas em `CommerceApiClientConfig` · `mapper/`: conversões · `exception/`: `GlobalExceptionHandler` com `ProblemDetail`

Mesmas regras do `commerce-api`: SOLID, injeção por construtor, DTOs como `record`, mensagens ao usuário em português, nada de segredo no código (variável de ambiente ou `.env` local, que não vai para o Git). Testes com JUnit 5 e MockMvc (`spring-security-test`).

**Fluxo de trabalho:** um endpoint por vez — ver no front o que a tela precisa → API → BFF (com Swagger) → trocar o mock do front → testar de ponta a ponta (inclusive com a API desligada). No back, o Claude propõe e passa uma classe por vez; o George cria, revisa e valida antes da próxima.

## Comandos

- `./mvnw spring-boot:run` — sobe o BFF na porta 8090 (o Keycloak roda pelo compose do `commerce-auth`)
- `./mvnw test` — testes (sem Docker)

## Git

Nunca executar add, commit, push ou comandos que alterem o histórico. Só sugerir, em uma linha com `&&`, terminando em `git pull --rebase origin develop && git push origin develop`. Mensagens em português, sem prefixo semântico. Ambiente: Windows com Git Bash.
