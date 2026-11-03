package br.com.george.commerce.bff.dto;

import lombok.Builder;

/** Corpo de {@code POST/PUT /me/addresses} na commerce-api. */
@Builder
public record EnderecoApiRequest(String street, String number, String complement, String neighborhood,
        String city, String state, String zipCode, Boolean primaryAddress) {
}
