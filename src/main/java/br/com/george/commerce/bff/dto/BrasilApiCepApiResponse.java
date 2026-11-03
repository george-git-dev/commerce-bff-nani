package br.com.george.commerce.bff.dto;

/** Resposta da BrasilAPI v2 (só os campos usados). */
public record BrasilApiCepApiResponse(String street, String neighborhood, String city, String state) {
}
