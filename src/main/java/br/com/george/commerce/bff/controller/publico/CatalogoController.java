package br.com.george.commerce.bff.controller.publico;

import br.com.george.commerce.bff.dto.CatalogoFiltrosResponse;
import br.com.george.commerce.bff.dto.CatalogoPaginaResponse;
import br.com.george.commerce.bff.dto.CatalogoProdutoResponse;
import br.com.george.commerce.bff.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pública: a vitrine é a mesma para visitante e cliente logado. */
@Tag(name = "Público - Vitrine", description = "Produtos publicados da loja.")
@RestController
@RequestMapping("/bff/catalog")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @Operation(summary = "Buscar produtos (filtros, ordem e página)", description = """
            Parâmetros (todos opcionais; listas separadas por vírgula valem como "ou"): category, family,
            occasion, gender, brand, minPrice, maxPrice, q, inStock, kind, deal, launch, bestSeller, badge,
            sort (relevance | price_asc | price_desc | best_sellers | newest), page (começa em 0), size (1 a 60).""")
    @GetMapping("/products")
    public ResponseEntity<CatalogoPaginaResponse> buscar(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parametros) {
        return ResponseEntity.ok(catalogoService.buscar(parametros));
    }

    @Operation(summary = "Produto pelo slug")
    @GetMapping("/products/{slug}")
    public ResponseEntity<CatalogoProdutoResponse> buscarPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(catalogoService.buscarPorSlug(slug));
    }

    @Operation(summary = "Opções de filtro")
    @GetMapping("/filters")
    public ResponseEntity<CatalogoFiltrosResponse> filtros() {
        return ResponseEntity.ok(catalogoService.filtros());
    }
}
