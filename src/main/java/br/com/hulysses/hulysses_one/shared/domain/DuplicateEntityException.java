package br.com.hulysses.hulysses_one.shared.domain;

public class DuplicateEntityException extends RuntimeException {

    public DuplicateEntityException(String entityName, Long id) {
        super(entityName + " already exists with id: " + id);
    }
}