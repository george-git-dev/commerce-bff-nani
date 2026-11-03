package br.com.george.commerce.bff.dto;

/** Endereço como a commerce-api devolve em {@code /me/addresses} (contrato da API, não do front). */
public record EnderecoApiResponse(Long id, String street, String number, String complement, String neighborhood,
        String city, String state, String zipCode, Boolean primaryAddress) {
}
