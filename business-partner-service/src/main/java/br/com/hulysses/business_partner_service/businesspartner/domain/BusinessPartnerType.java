package br.com.hulysses.business_partner_service.businesspartner.domain;

public enum BusinessPartnerType {
    INDIVIDUAL("Pessoa"),
    COMPANY("Empresa");

    private final String displayName;

    BusinessPartnerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}