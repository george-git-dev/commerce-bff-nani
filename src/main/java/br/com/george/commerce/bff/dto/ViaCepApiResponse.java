package br.com.george.commerce.bff.dto;

/** Resposta do ViaCEP (só os campos usados). {@code erro} preenchido = CEP não existe. */
public record ViaCepApiResponse(String logradouro, String bairro, String localidade, String uf, Object erro) {
}
