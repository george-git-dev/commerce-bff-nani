package br.com.george.commerce.bff.dto;

/**
 * Tela Dados pessoais → "Salvar dados". A validação de verdade é da commerce-api (erros por campo
 * voltam como vieram). Campo {@code null} não muda nada.
 *
 * @param phone celular com DDD, só números; vazio apaga
 * @param cpf   só aceito enquanto a conta não tem CPF
 */
public record AtualizarPerfilRequest(String phone, String cpf) {
}
