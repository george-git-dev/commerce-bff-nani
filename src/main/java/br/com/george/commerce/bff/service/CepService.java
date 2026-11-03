package br.com.george.commerce.bff.service;

import br.com.george.commerce.bff.dto.CepResponse;

public interface CepService {

    /** Endereço do CEP (com ou sem máscara). */
    CepResponse buscar(String cep);
}
