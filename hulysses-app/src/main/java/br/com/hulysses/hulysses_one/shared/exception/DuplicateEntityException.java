package br.com.hulysses.hulysses_one.shared.exception;

public class DuplicateEntityException extends DomainException {

    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(String entityName, Long id) {
        super(entityName + " already exists with id: " + id);
    }
}
