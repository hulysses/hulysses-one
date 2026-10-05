package br.com.hulysses.hulysses_one.shared.exception;

public class ExternalServiceUnavailableException extends RuntimeException {
    public ExternalServiceUnavailableException(Throwable cause) {
        super("Business partner service is currently unavailable", cause);
    }
}
