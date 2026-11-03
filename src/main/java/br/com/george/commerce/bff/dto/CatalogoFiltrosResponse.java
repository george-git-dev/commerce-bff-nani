package br.com.george.commerce.bff.dto;

import java.math.BigDecimal;
import java.util.List;

/** Opções de filtro que existem nos produtos publicados. */
public record CatalogoFiltrosResponse(List<String> categories, List<String> brands, List<String> families,
        List<String> occasions, List<String> genders, List<String> badges, BigDecimal minPrice,
        BigDecimal maxPrice) {
}
