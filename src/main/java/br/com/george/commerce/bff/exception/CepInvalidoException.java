package br.com.george.commerce.bff.exception;

import org.springframework.http.HttpStatus;

public class CepInvalidoException extends BusinessException {

    public CepInvalidoException() {
        super(HttpStatus.BAD_REQUEST, "CEP_INVALID", "CEP inválido. Confira os 8 números.");
    }
}
