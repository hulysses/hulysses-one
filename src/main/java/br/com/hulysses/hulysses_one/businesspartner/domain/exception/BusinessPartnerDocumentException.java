package br.com.hulysses.hulysses_one.businesspartner.domain.exception;

import br.com.hulysses.hulysses_one.shared.exception.DomainException;

public class BusinessPartnerDocumentException
        extends DomainException {

    public BusinessPartnerDocumentException(String message) {
        super(message);
    }
}