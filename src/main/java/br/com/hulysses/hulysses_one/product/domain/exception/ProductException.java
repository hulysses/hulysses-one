package br.com.hulysses.hulysses_one.product.domain.exception;

import br.com.hulysses.hulysses_one.shared.domain.DomainException;

public class ProductException extends DomainException {

    public ProductException(String message) {
        super(message);
    }
}