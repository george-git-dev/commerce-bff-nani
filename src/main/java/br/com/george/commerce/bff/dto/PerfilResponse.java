package br.com.george.commerce.bff.dto;

/**
 * Tela Minha conta → Dados pessoais. Junta as duas partes do cliente (decisão 1 do contrato):
 * nome e e-mail do Keycloak (a sessão), CPF e celular da commerce-api.
 *
 * @param cpf   mascarado ({@code ***.982.247-**}); {@code null} se ainda não informado
 * @param phone celular com DDD, só números; {@code null} se não informado
 */
public record PerfilResponse(String name, String email, String cpf, String phone) {
}
