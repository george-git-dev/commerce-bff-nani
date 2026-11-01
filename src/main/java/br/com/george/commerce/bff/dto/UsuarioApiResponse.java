package br.com.george.commerce.bff.dto;

/**
 * Perfil da loja como a commerce-api devolve em {@code GET /me} (contrato da API, não do front).
 *
 * @param cpf   já mascarado pela API ({@code ***.982.247-**}); {@code null} se ainda não informado
 * @param phone celular com DDD, só números; {@code null} se não informado
 */
public record UsuarioApiResponse(Long id, String name, String cpf, String email, String phone, Boolean active) {
}
