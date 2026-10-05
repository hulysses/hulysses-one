package br.com.hulysses.business_partner_service.shared.exception;

public class DuplicateEntityException extends DomainException {

    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(String entityName, Long id) {
        super(entityName + " already exists with id: " + id);
    }
}
