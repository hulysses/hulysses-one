package br.com.hulysses.hulysses_one.businesspartner.domain;

public enum BusinessPartnerRole {
    CUSTOMER("Cliente"),
    SUPPLIER("Fornecedor"),
    EMPLOYEE("Funcionário"),
    USER("Usuário");

    private final String displayName;

    BusinessPartnerRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
