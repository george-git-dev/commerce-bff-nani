package br.com.george.commerce.bff.exception;

import org.springframework.http.HttpStatus;

public class CepNaoEncontradoException extends BusinessException {

    public CepNaoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "CEP_NOT_FOUND", "Não encontramos esse CEP. Confira ou preencha o endereço.");
    }
}
