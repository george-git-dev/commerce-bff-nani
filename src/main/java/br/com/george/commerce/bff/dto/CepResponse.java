package br.com.george.commerce.bff.dto;

import lombok.Builder;

/**
 * Endereço do CEP para preencher o formulário (o cliente sempre pode corrigir). CEP de cidade
 * pequena pode vir sem rua e bairro (strings vazias).
 */
@Builder
public record CepResponse(String cep, String street, String district, String city, String state) {
}
