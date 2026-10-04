package br.com.hulysses.hulysses_one.businesspartner.domain.exception;

import br.com.hulysses.hulysses_one.shared.exception.DomainException;

public class AddressException extends DomainException {

    public AddressException(String message) {
        super(message);
    }
}