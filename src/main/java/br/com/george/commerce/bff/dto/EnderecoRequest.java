package br.com.george.commerce.bff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minha conta → Endereços → "Salvar endereço" (nomes do front). A validação de verdade é da
 * commerce-api: erros por campo voltam como vieram.
 *
 * @param isDefault usar como endereço principal
 */
public record EnderecoRequest(String cep, String street, String number, String complement, String district,
        String city, String state, @JsonProperty("isDefault") Boolean isDefault) {
}
