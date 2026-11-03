package br.com.george.commerce.bff.dto;

import lombok.Builder;
/** Corpo de {@code PATCH /me} na commerce-api (contrato da API, não do front). */
@Builder
public record AtualizarPerfilApiRequest(String phone, String cpf) {
}
