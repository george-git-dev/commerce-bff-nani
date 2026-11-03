package br.com.george.commerce.bff.exception;

import org.springframework.http.HttpStatus;

public class CepIndisponivelException extends BusinessException {

    public CepIndisponivelException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "A busca de CEP está fora do ar agora. Preencha o endereço, por favor.");
    }
}
