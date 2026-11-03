package br.com.george.commerce.bff.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro de regra do PRÓPRIO BFF (os da commerce-api passam como vieram), no mesmo formato da API:
 * status, {@code code} estável e frase em português. O GlobalExceptionHandler cuida do resto.
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
