package br.com.george.commerce.bff.dto;

import java.util.List;

/**
 * Produto da vitrine. A commerce-api já devolve no contrato da loja ({@code Product} do front),
 * então o BFF repassa sem traduzir.
 */
public record CatalogoProdutoResponse(
        Long id,
        String slug,
        String line,
        String subtitle,
        String name,
        String description,
        Long brandId,
        String brandName,
        String category,
        String gender,
        String concentration,
        List<String> families,
        CatalogoNotasResponse notes,
        List<String> kitItems,
        List<String> occasions,
        Boolean launch,
        Boolean bestSeller,
        String badge,
        Integer soldCount,
        List<String> images,
        List<CatalogoVarianteResponse> variants) {
}
