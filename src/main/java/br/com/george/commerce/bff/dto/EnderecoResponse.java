package br.com.george.commerce.bff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/** Endereço salvo, nos nomes do front ({@code SavedAddress}). */
@Builder
public record EnderecoResponse(Long id, String cep, String street, String number, String complement,
        String district, String city, String state, @JsonProperty("isDefault") Boolean isDefault) {
}
