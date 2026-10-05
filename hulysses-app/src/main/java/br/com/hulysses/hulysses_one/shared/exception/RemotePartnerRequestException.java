package br.com.hulysses.hulysses_one.shared.exception;

import java.util.Map;

public class RemotePartnerRequestException extends RuntimeException {
    private final int status;
    private final Map<String, String> fields;

    public RemotePartnerRequestException(int status, Map<String, String> fields) {
        super(switch (status) {
            case 404 -> "Business partner not found";
            case 409 -> "Operation conflicts with an existing business partner";
            default -> "Business partner request validation failed";
        });
        this.status = status;
        this.fields = fields == null ? Map.of() : Map.copyOf(fields);
    }

    public int status() { return status; }
    public Map<String, String> fields() { return fields; }
}
