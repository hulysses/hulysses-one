package br.com.hulysses.business_partner_service.businesspartner.domain.exception;

import br.com.hulysses.business_partner_service.shared.exception.DomainException;

public class AddressException extends DomainException {

    public AddressException(String message) {
        super(message);
    }
}