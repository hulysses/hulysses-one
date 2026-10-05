package br.com.hulysses.business_partner_service.businesspartner.domain.exception;

import br.com.hulysses.business_partner_service.shared.exception.DomainException;

public class BusinessPartnerDocumentException
        extends DomainException {

    public BusinessPartnerDocumentException(String message) {
        super(message);
    }
}