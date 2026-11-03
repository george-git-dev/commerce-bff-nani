package br.com.george.commerce.bff.client;

import br.com.george.commerce.bff.dto.CatalogoFiltrosResponse;
import br.com.george.commerce.bff.dto.CatalogoPaginaResponse;
import br.com.george.commerce.bff.dto.CatalogoProdutoResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Vitrine pública da commerce-api ({@code /catalog}). SEM token: é a mesma para todo mundo, logado
 * ou não (e o token do cliente não precisa sair para onde não é necessário).
 */
@FeignClient(name = "catalogo", url = "${nani.api.url}")
public interface CatalogoClient {

    /** Filtros já filtrados pela lista fechada do BFF; listas separadas por vírgula. */
    @GetMapping("/catalog/products")
    CatalogoPaginaResponse buscar(@RequestParam Map<String, String> filtros);

    @GetMapping("/catalog/products/{slug}")
    CatalogoProdutoResponse buscarPorSlug(@PathVariable("slug") String slug);

    @GetMapping("/catalog/filters")
    CatalogoFiltrosResponse filtros();
}
