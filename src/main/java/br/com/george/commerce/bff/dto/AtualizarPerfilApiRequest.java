package br.com.george.commerce.bff.dto;

/** Corpo de {@code PATCH /me} na commerce-api (contrato da API, não do front). */
public record AtualizarPerfilApiRequest(String phone, String cpf) {
}
