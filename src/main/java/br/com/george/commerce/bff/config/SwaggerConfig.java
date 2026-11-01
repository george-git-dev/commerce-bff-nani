package br.com.george.commerce.bff.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger do BFF (http://localhost:8090/swagger-ui.html). O BFF não usa token: a autenticação é o
 * cookie de sessão NANI_SESSION, criado ao entrar pelo site. Para testar rota que exige login no
 * "Try it out", entre antes pelo site (localhost:4200): o cookie vale para o localhost todo.
 */
@Configuration
public class SwaggerConfig {

    private static final String SESSION_SCHEME = "sessao";

    @Bean
    OpenAPI bffOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BFF da Nani Perfumes")
                        .version("1.0")
                        .description("""
                                Rotas que o site da Nani usa. Login, cadastro e sessão ficam aqui; o resto chama a \
                                commerce-api com o token da sessão. Erros no formato ProblemDetail: status, code e \
                                detail em português (os da commerce-api passam como vieram).
                                Escrita (POST/PUT/PATCH/DELETE) exige o cabeçalho X-XSRF-TOKEN com o valor do \
                                cookie XSRF-TOKEN."""))
                .addServersItem(new Server().url("http://localhost:4200").description("Pelo site (proxy do Angular)"))
                .addServersItem(new Server().url("http://localhost:8090").description("BFF direto"))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_SCHEME))
                .components(new Components().addSecuritySchemes(SESSION_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("NANI_SESSION")
                        .description("Cookie de sessão HttpOnly, criado ao entrar pelo site")));
    }

    /**
     * Respostas que valem para todas as rotas, num lugar só (os controllers ficam só com o resumo):
     * erros do próprio BFF e o 302 das rotas que levam ao Keycloak.
     */
    @Bean
    OpenApiCustomizer respostasPadrao() {
        return openApi -> openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
            ApiResponses responses = operation.getResponses();
            if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
                responses.clear();
                responses.addApiResponse("302", new ApiResponse().description("Vai para a tela do Keycloak"));
                return;
            }
            if (!path.startsWith("/api/auth/")) {
                responses.addApiResponse("401", new ApiResponse().description("Sem sessão (code SESSION_EXPIRED)"));
            }
            responses.addApiResponse("502", new ApiResponse()
                    .description("commerce-api fora do ar ou lenta (code SERVICE_UNAVAILABLE)"));
        }));
    }
}
