# CLAUDE.md

Orienta o Claude ao trabalhar neste repositório.

## O que é

`commerce-bff-nani`: BFF da Nani Perfumes (Java 21, Spring Boot 4.1). Arquitetura completa no projeto: `claude/roadmap-mvp.md` ("Arquitetura" e "Fluxo entre serviços") e `claude/contrato-api.md`.

- **Fala só com o Keycloak (login) e com a `commerce-api`.** Nunca com a `commerce-payments`.
- **Sem regra de negócio.** Repassa e monta respostas para as telas; quem decide é a `commerce-api`.
- **Token nunca vai para o navegador.** Sessão **em memória** (decisão A1: tokens nunca em disco; Redis só quando houver mais de uma cópia); **o BFF não tem banco**; cookie `NANI_SESSION` HttpOnly; CSRF por cookie `XSRF-TOKEN` + cabeçalho `X-XSRF-TOKEN`.
- `/api/**` sem sessão responde **401**, nunca redireciona (o Angular decide).
- Formatos e nomes seguem o front (`commerce-web`): ele manda.

## Convenções

Pacote base `br.com.george.commerce.bff`, **por camada, como o `commerce-api`**:

- `config/`: segurança, cliente OIDC, propriedades
- `security/`: peças do login (PKCE/cadastro, para onde voltar, logout, CSRF)
- `controller/`: só recebe e delega; nomes e pacotes em português, por público (`publico/`, `me/`, `admin/`)
- `client/`: clientes HTTP para a `commerce-api` (a partir da B3)
- `service/`: interface + `Impl`; junta e repassa o que vem do `client` (sem regra de negócio)
- `dto/`: records de request/response · `mapper/`: conversões · `exception/`: `GlobalExceptionHandler` com `ProblemDetail`

Mesmas regras do `commerce-api`: SOLID, injeção por construtor, DTOs como `record`, mensagens ao usuário em português, nada de segredo no código (variável de ambiente ou `.env` local, que não vai para o Git). Testes com JUnit 5 e MockMvc (`spring-security-test`).

**Fluxo de trabalho no back:** o Claude propõe e passa uma classe por vez; o George cria, revisa e valida antes da próxima.

## Comandos

- `./mvnw spring-boot:run` — sobe o BFF na porta 8090 (o Keycloak roda pelo compose do `commerce-auth`)
- `./mvnw test` — testes (sem Docker)

## Git

Nunca executar add, commit, push ou comandos que alterem o histórico. Só sugerir, em uma linha com `&&`, terminando em `git pull --rebase origin develop && git push origin develop`. Mensagens em português, sem prefixo semântico. Ambiente: Windows com Git Bash.
