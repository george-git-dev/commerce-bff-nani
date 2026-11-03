package br.com.george.commerce.bff.dto;

import java.math.BigDecimal;

/** Um tamanho do produto ({@code id} = SKU; {@code promoPrice} já calculado pela API). */
public record CatalogoVarianteResponse(String id, String kind, Integer volumeMl, BigDecimal price,
        BigDecimal promoPrice, Integer stock) {
}
