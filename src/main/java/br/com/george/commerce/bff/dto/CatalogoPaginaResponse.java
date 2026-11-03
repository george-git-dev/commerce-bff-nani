package br.com.george.commerce.bff.dto;

import java.util.List;

/** Uma página do catálogo ({@code page} começa em 0). */
public record CatalogoPaginaResponse(List<CatalogoProdutoResponse> items, int page, int size, long totalItems,
        int totalPages) {
}
